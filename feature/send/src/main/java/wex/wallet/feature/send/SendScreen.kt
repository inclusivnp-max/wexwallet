@file:Suppress("MagicNumber")

package wex.wallet.feature.send

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import wex.wallet.core.designsystem.component.GlassCard
import wex.wallet.core.designsystem.component.IconTile
import wex.wallet.core.designsystem.component.Skeleton
import wex.wallet.core.designsystem.component.SuccessCheck
import wex.wallet.core.designsystem.component.WexAddressShort
import wex.wallet.core.designsystem.component.WexGhostButton
import wex.wallet.core.designsystem.component.WexNotice
import wex.wallet.core.designsystem.component.WexNoticeTone
import wex.wallet.core.designsystem.component.WexPill
import wex.wallet.core.designsystem.component.WexPrimaryButton
import wex.wallet.core.designsystem.component.WexSecondaryButton
import wex.wallet.core.designsystem.component.WexStepper
import wex.wallet.core.designsystem.component.WexTextField
import wex.wallet.core.designsystem.format.WexFormat
import wex.wallet.core.designsystem.motion.Motion
import wex.wallet.core.designsystem.motion.ScreenTransitions
import wex.wallet.core.designsystem.theme.WexBorderWidth
import wex.wallet.core.designsystem.theme.WexTheme
import wex.wallet.core.network.JsonRpcException
import java.math.BigDecimal
import java.math.BigInteger

private enum class SendStep { ADDRESS, AMOUNT, CONFIRM, SENDING, DONE }

private const val RECEIPT_POLL_ATTEMPTS = 60
private const val RECEIPT_POLL_INTERVAL_MS = 3_000L
private const val SENDING_STAGE_DELAY_MS = 1_400L
private const val REVEAL_STEP_MS = 130L
private const val REVEAL_STAGES = 3
private const val DANGER_INITIAL_SCALE = 0.95f

/** Шагов в потоке: получатель, сумма, подтверждение, результат. */
private const val SEND_STEP_COUNT = 4

/** Комиссия в EVM-сетях всегда платится нативной монетой, а не отправляемым токеном. */
private const val FEE_SYMBOL = "ETH"

private const val WEI_DECIMALS = 18

private val SENDING_STAGES = listOf("Подписываем", "Отправляем в сеть", "Ждём подтверждения")

/** Галочка успеха — главный объект экрана, поэтому крупнее любой другой отметки. */
private val SuccessMarkSize: Dp = WexTheme.spacing.giant + WexTheme.spacing.xxxl

/** Заглушка на месте значения: ширина взята от самой длинной строки, чтобы карточка не прыгала. */
private val PlaceholderValueWidth: Dp = WexTheme.spacing.giant + WexTheme.spacing.xxxl

/** Владелец состояния потока: шаги, введённые значения и направление последнего перехода. */
@Stable
private class SendFlowState {
    var step by mutableStateOf(SendStep.ADDRESS)
        private set
    var previousStep by mutableStateOf(SendStep.ADDRESS)
        private set
    var toAddress by mutableStateOf("")
    var amount by mutableStateOf("")
    var gasEstimate by mutableStateOf<GasEstimate?>(null)
    var txHash by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    /** Возврат по потоку должен ехать в обратную сторону, поэтому сравниваем порядок шагов. */
    val isForward: Boolean get() = step.ordinal >= previousStep.ordinal

    fun goTo(next: SendStep) {
        previousStep = step
        step = next
    }
}

/** Контекст экрана: то, что приходит снаружи и не меняется по ходу шагов. */
private data class SendEnv(
    val currentBalance: String,
    val asset: SendAsset,
    val nativeBalanceWei: BigInteger,
    val recentAddresses: List<String>,
    val nativePriceUsd: BigDecimal?,
)

/** Внешние операции: оценка газа, отправка и работа с зависшей транзакцией. */
private class SendCallbacks(
    val onEstimateGas: suspend (String, String, Boolean) -> GasEstimate?,
    val onSend: suspend (String, String, GasEstimate) -> String?,
    val onSpeedUp: (suspend (PendingTx) -> String?)?,
    val onCancel: (suspend (PendingTx) -> String?)?,
    val onCheckReceipt: (suspend (String) -> TxStatus)?,
    val onDone: () -> Unit,
)

/**
 * Поток отправки.
 *
 * @param nativePriceUsd курс нативной монеты. Нужен, чтобы комиссия показывалась
 *   в двух единицах: «0,0004 ETH» само по себе не говорит, дорого это или нет.
 */
@Suppress("LongParameterList")
@Composable
fun SendScreen(
    currentBalance: String,
    asset: SendAsset = SendAsset(symbol = "ETH", name = "Ethereum"),
    nativeBalanceWei: BigInteger = BigInteger.ZERO,
    onEstimateGas: suspend (to: String, amount: String, isToken: Boolean) -> GasEstimate?,
    onSend: suspend (to: String, amount: String, gas: GasEstimate) -> String?,
    onSpeedUp: (suspend (PendingTx) -> String?)? = null,
    onCancel: (suspend (PendingTx) -> String?)? = null,
    onCheckReceipt: (suspend (String) -> TxStatus)? = null,
    recentAddresses: List<String> = emptyList(),
    nativePriceUsd: BigDecimal? = null,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val flow = remember { SendFlowState() }

    Column(modifier = modifier.fillMaxSize().padding(WexTheme.spacing.screen)) {
        SendHeader(flow.step)
        Spacer(Modifier.height(WexTheme.spacing.md))
        WexStepper(totalSteps = SEND_STEP_COUNT, currentStep = flow.step.stepperIndex)
        Spacer(Modifier.height(WexTheme.spacing.xl))
        SendStepSwitcher(
            flow = flow,
            env = SendEnv(
                currentBalance = currentBalance,
                asset = asset,
                nativeBalanceWei = nativeBalanceWei,
                recentAddresses = recentAddresses,
                nativePriceUsd = nativePriceUsd,
            ),
            callbacks = SendCallbacks(
                onEstimateGas = onEstimateGas,
                onSend = onSend,
                onSpeedUp = onSpeedUp,
                onCancel = onCancel,
                onCheckReceipt = onCheckReceipt,
                onDone = onDone,
            ),
        )
    }
}

@Composable
private fun SendStepSwitcher(flow: SendFlowState, env: SendEnv, callbacks: SendCallbacks) {
    AnimatedContent(
        targetState = flow.step,
        transitionSpec = {
            if (flow.isForward) ScreenTransitions.forward() else ScreenTransitions.backward()
        },
        label = "sendStep",
    ) { step ->
        SendStepContent(
            state = flow.uiState(step, env),
            actions = flow.actions(callbacks),
        )
    }
}

@Composable
private fun SendHeader(step: SendStep) {
    Text(
        text = when (step) {
            SendStep.ADDRESS -> "Кому отправить"
            SendStep.AMOUNT -> "Сколько отправить"
            SendStep.CONFIRM -> "Проверьте перевод"
            SendStep.SENDING -> "Отправляем"
            SendStep.DONE -> "Перевод"
        },
        style = WexTheme.type.title,
        color = WexTheme.colors.ink,
    )
}

/**
 * Отправка идёт из четырёх шагов; SENDING — часть подтверждения, своего сегмента у него нет.
 * На результате пройдены все сегменты: возвращаться уже некуда.
 */
private val SendStep.stepperIndex: Int
    get() = when (this) {
        SendStep.ADDRESS -> 0
        SendStep.AMOUNT -> 1
        SendStep.CONFIRM, SendStep.SENDING -> 2
        SendStep.DONE -> SEND_STEP_COUNT
    }

/** Значения, из которых рисуется текущий шаг. */
private data class SendUiState(
    val step: SendStep,
    val toAddress: String,
    val amount: String,
    val currentBalance: String,
    val asset: SendAsset,
    val nativeBalanceWei: BigInteger,
    val gasEstimate: GasEstimate?,
    val txHash: String?,
    val error: String?,
    val recentAddresses: List<String>,
    val nativePriceUsd: BigDecimal?,
)

/** Действия, которые шаг может запросить у владельца состояния. */
@Suppress("LongParameterList")
private class SendActions(
    val onAddressChange: (String) -> Unit,
    val onAmountChange: (String) -> Unit,
    val onStepChange: (SendStep) -> Unit,
    val onGasEstimated: (GasEstimate) -> Unit,
    val onError: (String?) -> Unit,
    val onEstimateGas: suspend (String, String, Boolean) -> GasEstimate?,
    val onSend: suspend (String, String, GasEstimate) -> String?,
    val onSpeedUp: (suspend (PendingTx) -> String?)?,
    val onCancel: (suspend (PendingTx) -> String?)?,
    val onCheckReceipt: (suspend (String) -> TxStatus)?,
    val onTxHash: (String?) -> Unit,
    val onDone: () -> Unit,
)

private fun SendFlowState.uiState(step: SendStep, env: SendEnv) = SendUiState(
    step = step,
    toAddress = toAddress,
    amount = amount,
    currentBalance = env.currentBalance,
    asset = env.asset,
    nativeBalanceWei = env.nativeBalanceWei,
    gasEstimate = gasEstimate,
    txHash = txHash,
    error = error,
    recentAddresses = env.recentAddresses,
    nativePriceUsd = env.nativePriceUsd,
)

private fun SendFlowState.actions(callbacks: SendCallbacks) = SendActions(
    onAddressChange = { toAddress = it },
    onAmountChange = { amount = it },
    onStepChange = { goTo(it) },
    onGasEstimated = { gasEstimate = it },
    onError = { error = it },
    onEstimateGas = callbacks.onEstimateGas,
    onSend = callbacks.onSend,
    onSpeedUp = callbacks.onSpeedUp,
    onCancel = callbacks.onCancel,
    onCheckReceipt = callbacks.onCheckReceipt,
    onTxHash = { txHash = it },
    onDone = callbacks.onDone,
)

@Composable
private fun SendStepContent(state: SendUiState, actions: SendActions) {
    when (state.step) {
        SendStep.ADDRESS -> AddressStep(state, actions)
        SendStep.AMOUNT -> AmountStep(state, actions)
        SendStep.CONFIRM -> ConfirmStep(state, actions)
        SendStep.SENDING -> SendingStep(state, actions)
        SendStep.DONE -> DoneStep(state, actions)
    }
}

/** Пропускает дальше только валидный EVM-адрес, иначе показывает ошибку. */
private fun SendActions.validateAddress(address: String) {
    onError(null)
    if (isValidEvmAddress(address)) {
        onStepChange(SendStep.AMOUNT)
    } else {
        onError("Это не похоже на EVM-адрес: 42 знака, начиная с 0x")
    }
}

@Composable
private fun AddressStep(state: SendUiState, actions: SendActions) {
    Column {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            WexTextField(
                value = state.toAddress,
                onValueChange = actions.onAddressChange,
                label = "Адрес получателя",
                placeholder = "0x…",
                isError = state.error != null,
                errorText = state.error,
                monospace = true,
            )
        }
        Spacer(Modifier.height(WexTheme.spacing.beforeCta))
        WexPrimaryButton(
            text = "Далее",
            onClick = { actions.validateAddress(state.toAddress) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.toAddress.isNotBlank(),
        )
    }
}

@Composable
private fun AmountStep(state: SendUiState, actions: SendActions) {
    val symbol = state.asset.symbol
    Column {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.md)) {
                DetailRow(label = "Доступно") { ValueText(balanceText(state)) }
                WexTextField(
                    value = state.amount,
                    onValueChange = actions.onAmountChange,
                    label = "Сумма",
                    placeholder = "0",
                    trailingIcon = {
                        Text(text = symbol, style = WexTheme.type.label, color = WexTheme.colors.ink3)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }
        Spacer(Modifier.height(WexTheme.spacing.beforeCta))
        StepNav(
            onBack = { actions.onStepChange(SendStep.ADDRESS) },
            onNext = { actions.onStepChange(SendStep.CONFIRM) },
            nextText = "Далее",
            nextEnabled = state.amount.toBigDecimalOrNull() != null,
        )
    }
}

@Composable
private fun ConfirmStep(state: SendUiState, actions: SendActions) {
    var loading by remember { mutableStateOf(state.gasEstimate == null) }
    var gasCheck by remember { mutableStateOf<GasPrecheckResult?>(null) }

    if (loading) {
        LaunchedEffect(state.toAddress, state.amount) {
            try {
                val estimate = actions.onEstimateGas(state.toAddress, state.amount, !state.asset.isNative)
                if (estimate != null) {
                    actions.onGasEstimated(estimate)
                    gasCheck = runGasPrecheck(state.asset, state.nativeBalanceWei, state.amount, estimate)
                } else {
                    actions.onError("Сеть не ответила на запрос комиссии")
                }
            } catch (e: JsonRpcException) {
                actions.onError(humanizeRpcError(e.message))
            }
            loading = false
        }
        EstimatingGas()
        return
    }

    ConfirmDetails(state = state, gasCheck = gasCheck, actions = actions)
}

/** Пока комиссия неизвестна, место под неё уже занято: цифра придёт, и карточка не прыгнет. */
@Composable
private fun EstimatingGas() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Считаем комиссию сети",
                style = WexTheme.type.label,
                color = WexTheme.colors.ink3,
            )
            DetailRow(label = "Получателю") { Skeleton(width = PlaceholderValueWidth) }
            RowDivider()
            DetailRow(label = "Сумма") { Skeleton(width = PlaceholderValueWidth) }
            RowDivider()
            DetailRow(label = "Комиссия") { Skeleton(width = PlaceholderValueWidth) }
        }
    }
}

@Composable
private fun ConfirmDetails(state: SendUiState, gasCheck: GasPrecheckResult?, actions: SendActions) {
    val warnings = remember(state.toAddress, state.recentAddresses) {
        ScamChecker.check(
            toAddress = state.toAddress,
            isFirstTransfer = state.recentAddresses.none { it.equals(state.toAddress, ignoreCase = true) },
            recentAddresses = state.recentAddresses,
        )
    }
    val hasDanger = warnings.any { it.level == ScamWarning.Level.DANGER }
    val shortfall = gasCheck?.takeIf { !it.hasEnoughGas }

    Column(verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.cardGap)) {
        ConfirmCard(state)
        shortfall?.let { GasShortfallNotice(it) }
        state.error?.let { SendErrorNotice(it) }
        ConfirmWarnings(warnings)
        Spacer(Modifier.height(WexTheme.spacing.sm))
        StepNav(
            onBack = { actions.onStepChange(SendStep.AMOUNT) },
            onNext = { actions.onStepChange(SendStep.SENDING) },
            nextText = if (hasDanger) "Всё равно отправить" else "Отправить",
            nextEnabled = state.gasEstimate != null && state.error == null && shortfall == null,
        )
    }
}

private fun runGasPrecheck(
    asset: SendAsset,
    nativeBalanceWei: BigInteger,
    amount: String,
    gas: GasEstimate,
): GasPrecheckResult {
    val sendWei = if (asset.isNative) {
        BigDecimal(amount)
            .multiply(BigDecimal.TEN.pow(asset.decimals))
            .toBigInteger()
    } else {
        BigInteger.ZERO
    }
    return GasPrecheck.check(
        balanceWei = nativeBalanceWei,
        sendAmountWei = sendWei,
        estimatedGasWei = gas.totalMaxFeeWei,
        symbol = FEE_SYMBOL,
    )
}

@Composable
private fun ConfirmCard(state: SendUiState) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            DetailRow(label = "Получателю") { WexAddressShort(address = state.toAddress) }
            RowDivider()
            DetailRow(label = "Сумма") { ValueText(amountText(state)) }
            RowDivider()
            DetailRow(label = "Комиссия") { ValueText(feeText(state)) }
            RowDivider()
            DetailRow(label = "Итого") { ValueText(totalText(state), strong = true) }
        }
    }
}

/** Строка «подпись — значение». Значение слот, потому что это и адрес, и сумма, и капсула. */
@Composable
private fun DetailRow(label: String, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = WexTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Место занимает подпись: значение важнее и сжиматься не должно.
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = WexTheme.type.body,
            color = WexTheme.colors.ink3,
            maxLines = 1,
        )
        Spacer(Modifier.width(WexTheme.spacing.md))
        value()
    }
}

/** Линия между строками: тоньше её не бывает, а без неё цифры сливаются в столбик. */
@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(WexBorderWidth)
            .background(WexTheme.colors.edgeFaint),
    )
}

@Composable
private fun ValueText(text: String, strong: Boolean = false) {
    Text(
        text = text,
        style = if (strong) WexTheme.type.amountSecondary else WexTheme.type.body,
        color = if (strong) WexTheme.colors.ink else WexTheme.colors.ink2,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun balanceText(state: SendUiState): String {
    val value = state.currentBalance.toBigDecimalOrNull() ?: return state.currentBalance
    return WexFormat.amount(value, state.asset.symbol)
}

/** В подтверждении сумма показывается целиком: подписывают точное значение, а не округлённое. */
private fun amountText(state: SendUiState): String {
    val value = state.amount.toBigDecimalOrNull() ?: return state.amount
    return WexFormat.amount(value, state.asset.symbol, state.asset.decimals)
}

private fun feeText(state: SendUiState): String {
    val fee = state.gasEstimate?.totalMaxFeeEth ?: return "—"
    return WexFormat.fee(fee, FEE_SYMBOL, state.nativePriceUsd?.multiply(fee))
}

/** Итог: сколько уйдёт со счёта. Токен и газ живут в разных монетах и не складываются. */
private fun totalText(state: SendUiState): String {
    val amount = state.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val fee = state.gasEstimate?.totalMaxFeeEth ?: BigDecimal.ZERO
    return if (state.asset.isNative) {
        WexFormat.amount(amount + fee, state.asset.symbol, state.asset.decimals)
    } else {
        WexFormat.amount(amount, state.asset.symbol, state.asset.decimals) +
            " + " + WexFormat.amount(fee, FEE_SYMBOL)
    }
}

@Composable
private fun GasShortfallNotice(result: GasPrecheckResult) {
    val shortfall = BigDecimal(result.shortfall).movePointLeft(WEI_DECIMALS)
    WexNotice(
        title = "Не хватает на комиссию",
        tone = WexNoticeTone.Danger,
        message = "Нужно ещё ${WexFormat.amount(shortfall, FEE_SYMBOL)}. " +
            "Уменьшите сумму или пополните счёт.",
    )
}

@Composable
private fun SendErrorNotice(message: String) {
    WexNotice(
        title = "Отправить не получилось",
        tone = WexNoticeTone.Danger,
        message = message,
    )
}

@Composable
private fun SendingStep(state: SendUiState, actions: SendActions) {
    val gas = state.gasEstimate!!

    SendingProgress()

    LaunchedEffect(Unit) {
        try {
            actions.onTxHash(actions.onSend(state.toAddress, state.amount, gas))
            actions.onStepChange(SendStep.DONE)
        } catch (e: JsonRpcException) {
            actions.onError(humanizeRpcError(e.message))
            actions.onStepChange(SendStep.CONFIRM)
        }
    }
}

/** Стадии сменяются по времени: пользователь видит, что отправка идёт, а не подвисла. */
@Composable
private fun SendingProgress() {
    var stage by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        repeat(SENDING_STAGES.lastIndex) {
            delay(SENDING_STAGE_DELAY_MS)
            stage = it + 1
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.xl),
        ) {
            CircularProgressIndicator(
                color = WexTheme.colors.lime,
                trackColor = WexTheme.colors.surface3,
            )
            AnimatedContent(
                targetState = stage,
                transitionSpec = { ScreenTransitions.crossfade() },
                label = "sendingStage",
            ) { index ->
                Text(
                    text = SENDING_STAGES[index],
                    style = WexTheme.type.bodyLarge,
                    color = WexTheme.colors.ink,
                )
            }
        }
    }
}

@Composable
private fun DoneStep(state: SendUiState, actions: SendActions) {
    var txStatus by remember { mutableStateOf(TxStatus.PENDING) }
    val haptics = LocalHapticFeedback.current
    val pending = pendingTx(state)

    TrackReceipt(state.txHash, actions.onCheckReceipt, txStatus) { txStatus = it }

    // Деньги ушли — это событие, и подтвердить его стоит не только глазу.
    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DoneReveal(state.txHash, txStatus)

        if (txStatus == TxStatus.PENDING && pending != null) {
            Spacer(Modifier.height(WexTheme.spacing.lg))
            PendingActions(
                pending = pending,
                onSpeedUp = actions.onSpeedUp,
                onCancel = actions.onCancel,
            )
        }

        Spacer(Modifier.height(WexTheme.spacing.beforeCta))
        WexPrimaryButton(
            text = "Готово",
            onClick = actions.onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Замена по тому же nonce возможна, только пока известны хеш и цена газа исходной отправки. */
private fun pendingTx(state: SendUiState): PendingTx? {
    val hash = state.txHash
    val gas = state.gasEstimate
    if (hash == null || gas == null) return null
    return PendingTx(
        txHash = hash,
        chainId = state.asset.chainId,
        nonce = 0,
        to = "",
        value = BigInteger.ZERO,
        maxFeePerGas = gas.maxFeePerGas,
    )
}

/** Каскад: сначала галочка, затем заголовок, затем детали — глаз успевает пройти сверху вниз. */
@Composable
private fun DoneReveal(txHash: String?, txStatus: TxStatus) {
    var stage by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        repeat(REVEAL_STAGES) {
            stage = it + 1
            delay(REVEAL_STEP_MS)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RevealSlot(visible = stage >= 1) {
            if (txStatus == TxStatus.FAILED) FailedMark() else SuccessCheck(size = SuccessMarkSize)
        }
        Spacer(Modifier.height(WexTheme.spacing.lg))
        RevealSlot(visible = stage >= 2) {
            Text(
                text = doneTitle(txStatus),
                style = WexTheme.type.title,
                color = WexTheme.colors.ink,
            )
        }
        Spacer(Modifier.height(WexTheme.spacing.lg))
        RevealSlot(visible = stage >= 3) {
            TxMeta(txHash, txStatus)
        }
    }
}

@Composable
private fun RevealSlot(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = Motion.enter()) +
            slideInVertically(animationSpec = Motion.enter()) { height -> height / 3 },
    ) {
        content()
    }
}

@Composable
private fun FailedMark() {
    IconTile(
        size = SuccessMarkSize,
        shape = WexTheme.shapes.card,
        container = WexTheme.colors.dangerWash,
    ) {
        Text(text = "×", style = WexTheme.type.display, color = WexTheme.colors.danger)
    }
}

@Composable
private fun TxMeta(txHash: String?, txStatus: TxStatus) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            if (txHash != null) {
                DetailRow(label = "Хеш") { WexAddressShort(address = txHash, copyable = true) }
                RowDivider()
            }
            DetailRow(label = "Статус") {
                WexPill(text = statusLabel(txStatus), tone = statusTone(txStatus))
            }
        }
    }
}

/** Опрашивает receipt, пока транзакция не подтвердится или не отвалится. */
@Composable
private fun TrackReceipt(
    txHash: String?,
    onCheckReceipt: (suspend (String) -> TxStatus)?,
    current: TxStatus,
    onStatus: (TxStatus) -> Unit,
) {
    if (txHash == null || onCheckReceipt == null || current != TxStatus.PENDING) return

    LaunchedEffect(txHash) {
        repeat(RECEIPT_POLL_ATTEMPTS) {
            val status = onCheckReceipt(txHash)
            if (status != TxStatus.PENDING) {
                onStatus(status)
                return@LaunchedEffect
            }
            delay(RECEIPT_POLL_INTERVAL_MS)
        }
    }
}

/** Ускорить или отменить зависшую транзакцию — заменой по тому же nonce. */
@Composable
private fun PendingActions(
    pending: PendingTx,
    onSpeedUp: (suspend (PendingTx) -> String?)?,
    onCancel: (suspend (PendingTx) -> String?)?,
) {
    val scope = rememberCoroutineScope()
    var inProgress by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    fun run(label: String, action: suspend (PendingTx) -> String?) {
        inProgress = true
        scope.launch {
            result = runCatching { action(pending) }
                .fold(
                    onSuccess = { hash -> "$label: ${hash?.let(WexFormat::shortHash) ?: "—"}" },
                    onFailure = { it.message ?: "Не удалось" },
                )
            inProgress = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(WexTheme.touch.gap)) {
            if (onSpeedUp != null) {
                WexSecondaryButton(
                    text = "Ускорить",
                    onClick = { run("Замена", onSpeedUp) },
                    enabled = !inProgress,
                )
            }
            if (onCancel != null) {
                WexSecondaryButton(
                    text = "Отменить",
                    onClick = { run("Отмена", onCancel) },
                    enabled = !inProgress,
                )
            }
        }
        result?.let { WexNotice(title = it, tone = WexNoticeTone.Info) }
    }
}

/** Возврат не равен продолжению по весу, поэтому назад — призрачная кнопка. */
@Composable
private fun StepNav(
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextText: String,
    nextEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WexTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WexGhostButton(text = "Назад", onClick = onBack)
        WexPrimaryButton(
            text = nextText,
            onClick = onNext,
            modifier = Modifier.weight(1f),
            enabled = nextEnabled,
        )
    }
}

@Composable
private fun ConfirmWarnings(warnings: List<ScamWarning>) {
    if (warnings.isEmpty()) return

    val danger = warnings.filter { it.level == ScamWarning.Level.DANGER }
    val rest = warnings.filterNot { it.level == ScamWarning.Level.DANGER }

    Column(verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm)) {
        if (danger.isNotEmpty()) {
            DangerWarnings(danger)
        }
        for (warning in rest) {
            ScamNoticeItem(warning)
        }
    }
}

/** Опасность обязана перехватить внимание: лёгкий наезд и отклик в палец. */
@Composable
private fun DangerWarnings(warnings: List<ScamWarning>) {
    val haptics = LocalHapticFeedback.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        haptics.performHapticFeedback(HapticFeedbackType.Reject)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = Motion.enter()) +
            scaleIn(animationSpec = Motion.enter(), initialScale = DANGER_INITIAL_SCALE),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WexTheme.spacing.sm)) {
            for (warning in warnings) {
                ScamNoticeItem(warning)
            }
        }
    }
}

@Composable
private fun ScamNoticeItem(warning: ScamWarning) {
    val notice = warning.toNotice()
    WexNotice(
        title = notice.title,
        tone = warning.level.toTone(),
        message = notice.message,
    )
}
