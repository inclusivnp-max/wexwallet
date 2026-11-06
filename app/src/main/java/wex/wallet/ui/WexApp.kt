@file:Suppress("MagicNumber")

package wex.wallet.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import wex.wallet.BuildConfig
import wex.wallet.chain.bitcoin.BitcoinSegwitAddressDeriver
import wex.wallet.chain.bitcoin.BitcoinTaprootAddressDeriver
import wex.wallet.chain.evm.Erc20
import wex.wallet.chain.evm.EvmAddressDeriver
import wex.wallet.chain.evm.EvmNetwork
import wex.wallet.chain.evm.EvmSigner
import wex.wallet.chain.evm.EvmTransaction
import wex.wallet.chain.evm.toHex
import wex.wallet.chain.solana.SolanaAddressDeriver
import wex.wallet.chain.ton.TonAddressDeriver
import wex.wallet.core.common.SensitiveBytes
import wex.wallet.core.designsystem.background.AnimatedShaderBackground
import wex.wallet.core.designsystem.background.ShaderBackgroundSettings
import wex.wallet.core.designsystem.background.ShaderEffect
import wex.wallet.core.designsystem.motion.ScreenTransitions
import wex.wallet.core.network.CoinGeckoClient
import wex.wallet.core.network.EtherscanClient
import wex.wallet.core.network.EvmRpcClient
import wex.wallet.core.network.EvmRpcRegistry
import wex.wallet.data.accounts.AccountDeriver
import wex.wallet.data.activity.ActivityRepository
import wex.wallet.data.portfolio.PriceRepository
import wex.wallet.feature.activity.ActivityRoute
import wex.wallet.feature.home.AssetItem
import wex.wallet.feature.home.HomeScreen
import wex.wallet.feature.onboarding.CreateWalletScreen
import wex.wallet.feature.onboarding.ImportWalletScreen
import wex.wallet.feature.onboarding.SetPinScreen
import wex.wallet.feature.onboarding.WelcomeScreen
import wex.wallet.feature.receive.ReceiveScreen
import wex.wallet.feature.send.GasEstimate
import wex.wallet.feature.send.SendAsset
import wex.wallet.feature.send.SendScreen
import wex.wallet.feature.send.TxStatus
import wex.wallet.security.mnemonic.HdKeyDeriver
import wex.wallet.security.mnemonic.Mnemonic
import java.math.BigDecimal
import java.math.BigInteger

private enum class AppScreen {
    WELCOME, CREATE_WALLET, IMPORT_WALLET, SET_PIN,
    HOME, SEND, RECEIVE, ACTIVITY, GALLERY,
}

private val EVM_ENDPOINTS = mapOf(
    EvmNetwork.ETHEREUM.chainId to BuildConfig.RPC_ETHEREUM,
    EvmNetwork.BASE.chainId to BuildConfig.RPC_BASE,
    EvmNetwork.ARBITRUM.chainId to BuildConfig.RPC_ARBITRUM,
    EvmNetwork.OPTIMISM.chainId to BuildConfig.RPC_OPTIMISM,
    EvmNetwork.POLYGON.chainId to BuildConfig.RPC_POLYGON,
    EvmNetwork.BSC.chainId to BuildConfig.RPC_BSC,
    EvmNetwork.BASE_SEPOLIA.chainId to BuildConfig.RPC_BASE_SEPOLIA,
)

/**
 * Фон приложения.
 *
 * Значения намеренно низкие: фон — это текстура, а не отдельный слой контента.
 * На более высоких значениях крупные пятна съедают контраст текста, особенно
 * в светлой теме, где белая подложка уходит в серый.
 */
private val BACKGROUND_SETTINGS = ShaderBackgroundSettings(
    alphaPercent = 22,
    intensity = 60,
    speed = 35,
    effect = ShaderEffect.PARTICLES,
    effectDensity = 22,
    effectSize = 28,
    effectGlow = 30,
    blurRadius = 28,
)

@Composable
fun WexApp() {
    var screen by remember { mutableStateOf(AppScreen.WELCOME) }
    var mnemonicWords by remember { mutableStateOf(emptyList<String>()) }
    var addresses by remember { mutableStateOf(emptyMap<String, String>()) }
    var assets by remember { mutableStateOf(emptyList<AssetItem>()) }
    var sendAsset by remember { mutableStateOf<SendAsset?>(null) }
    var nativeBalanceWei by remember { mutableStateOf(BigInteger.ZERO) }
    var recentAddresses by remember { mutableStateOf(emptyList<String>()) }
    val rpcRegistry = remember { EvmRpcRegistry(EVM_ENDPOINTS) }
    val priceRepository = remember {
        PriceRepository(CoinGeckoClient(BuildConfig.COINGECKO_API_KEY))
    }
    val etherscan = remember { EtherscanClient(BuildConfig.ETHERSCAN_API_KEY) }
    val activityRepository = remember(etherscan) { ActivityRepository(etherscan) }
    DisposableEffect(etherscan) { onDispose { etherscan.close() } }

    AnimatedShaderBackground(settings = BACKGROUND_SETTINGS) {
        Scaffold(containerColor = Color.Transparent) { contentPadding ->
            AnimatedContent(
                targetState = screen,
                modifier = Modifier.padding(contentPadding),
                // Направление несёт смысл: вперёд экран приезжает справа, назад — слева.
                transitionSpec = {
                    if (targetState.ordinal >= initialState.ordinal) {
                        ScreenTransitions.forward()
                    } else {
                        ScreenTransitions.backward()
                    }
                },
                label = "screen",
            ) { current ->
                WexContent(
                    screen = current,
                    addresses = addresses,
                    assets = assets,
                    sendAsset = sendAsset ?: SendAsset(symbol = "ETH", name = "Ethereum"),
                    nativeBalanceWei = nativeBalanceWei,
                    recentAddresses = recentAddresses,
                    rpcRegistry = rpcRegistry,
                    priceRepository = priceRepository,
                    etherscan = etherscan,
                    activityRepository = activityRepository,
                    mnemonicWords = mnemonicWords,
                    onScreenChange = { screen = it },
                    onMnemonicSet = { mnemonicWords = it },
                    onAddressesSet = { addresses = it },
                    onAssetsUpdate = { assets = it },
                    onSendAssetSelect = { sendAsset = it },
                    onNativeBalanceUpdate = { nativeBalanceWei = it },
                    onRecipientUsed = { addr ->
                        if (recentAddresses.none { it.equals(addr, ignoreCase = true) }) {
                            recentAddresses = (listOf(addr) + recentAddresses).take(20)
                        }
                    },
                )
            }
        }
    }
}

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun WexContent(
    screen: AppScreen,
    addresses: Map<String, String>,
    assets: List<AssetItem>,
    sendAsset: SendAsset,
    nativeBalanceWei: BigInteger,
    recentAddresses: List<String>,
    rpcRegistry: EvmRpcRegistry,
    priceRepository: PriceRepository,
    etherscan: EtherscanClient,
    activityRepository: ActivityRepository,
    mnemonicWords: List<String>,
    onScreenChange: (AppScreen) -> Unit,
    onMnemonicSet: (List<String>) -> Unit,
    onAddressesSet: (Map<String, String>) -> Unit,
    onAssetsUpdate: (List<AssetItem>) -> Unit,
    onSendAssetSelect: (SendAsset) -> Unit,
    onNativeBalanceUpdate: (BigInteger) -> Unit,
    onRecipientUsed: (String) -> Unit,
) {
    when (screen) {
        AppScreen.WELCOME -> WelcomeScreen(
            onCreateWallet = { onScreenChange(AppScreen.CREATE_WALLET) },
            onImportWallet = { onScreenChange(AppScreen.IMPORT_WALLET) },
            // Витрина — инструмент разработки, в релизной сборке входа в неё нет.
            onOpenGallery = if (BuildConfig.DEBUG) {
                { onScreenChange(AppScreen.GALLERY) }
            } else {
                null
            },
        )
        AppScreen.CREATE_WALLET -> CreateWalletScreen(
            onProceed = { words ->
                onMnemonicSet(words)
                onScreenChange(AppScreen.SET_PIN)
            },
        )
        AppScreen.IMPORT_WALLET -> ImportWalletScreen(
            onProceed = { words ->
                onMnemonicSet(words)
                onScreenChange(AppScreen.SET_PIN)
            },
        )
        AppScreen.SET_PIN -> SetPinScreen(
            onPinSet = { pin ->
                val seed = Mnemonic.toSeed(mnemonicWords)
                onAddressesSet(deriveAllAddresses(seed))
                seed.close()
                pin.fill('0')
                onScreenChange(AppScreen.HOME)
            },
        )
        AppScreen.HOME -> {
            val evmAddress = addresses["Ethereum"] ?: ""
            HomeWithBalances(
                evmAddress = evmAddress,
                addresses = addresses,
                assets = assets,
                rpcRegistry = rpcRegistry,
                priceRepository = priceRepository,
                etherscan = etherscan,
                onActivity = { onScreenChange(AppScreen.ACTIVITY) },
                onAssetsUpdate = onAssetsUpdate,
                onNativeBalanceUpdate = onNativeBalanceUpdate,
                onSend = {
                    onSendAssetSelect(SendAsset(symbol = "ETH", name = "Ethereum"))
                    onScreenChange(AppScreen.SEND)
                },
                onReceive = { onScreenChange(AppScreen.RECEIVE) },
            )
        }
        AppScreen.SEND -> SendRoute(
            sendAsset = sendAsset,
            assets = assets,
            addresses = addresses,
            mnemonicWords = mnemonicWords,
            nativeBalanceWei = nativeBalanceWei,
            recentAddresses = recentAddresses,
            rpcRegistry = rpcRegistry,
            onRecipientUsed = onRecipientUsed,
            onDone = { onScreenChange(AppScreen.HOME) },
        )
        AppScreen.ACTIVITY -> ActivityRoute(
            address = addresses["Ethereum"] ?: "",
            nowSeconds = System.currentTimeMillis() / 1000,
            loadFeed = { activityRepository.load(it) },
            onItemClick = {},
            onBack = { onScreenChange(AppScreen.HOME) },
        )
        AppScreen.GALLERY -> ComponentGallery(onBack = { onScreenChange(AppScreen.WELCOME) })
        AppScreen.RECEIVE -> ReceiveScreen(
            address = addresses["Ethereum"] ?: "",
            chainName = "Ethereum",
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun SendRoute(
    sendAsset: SendAsset,
    assets: List<AssetItem>,
    addresses: Map<String, String>,
    mnemonicWords: List<String>,
    nativeBalanceWei: BigInteger,
    recentAddresses: List<String>,
    rpcRegistry: EvmRpcRegistry,
    onRecipientUsed: (String) -> Unit,
    onDone: () -> Unit,
) {
    val evmRpc = rpcRegistry.clientForChain(sendAsset.chainId)
    val currentAsset = assets.find { it.chainId == sendAsset.chainId }
    val fromAddress = addresses["Ethereum"] ?: ""

    SendScreen(
        currentBalance = currentAsset?.balance ?: "",
        asset = sendAsset,
        nativeBalanceWei = nativeBalanceWei,
        onEstimateGas = { to, amount, _ ->
            estimateGas(evmRpc, fromAddress, to, amount, sendAsset)
        },
        onSend = { to, amount, gas ->
            sendEvmTransaction(evmRpc, mnemonicWords, to, amount, gas, sendAsset)
                .also { onRecipientUsed(to) }
        },
        recentAddresses = recentAddresses,
        onCheckReceipt = { txHash ->
            val receipt = evmRpc.getTransactionReceipt(txHash)
            when {
                receipt == null -> TxStatus.PENDING
                receipt.status -> TxStatus.CONFIRMED
                else -> TxStatus.FAILED
            }
        },
        onDone = onDone,
    )
}

@Suppress("LongParameterList")
@Composable
private fun HomeWithBalances(
    evmAddress: String,
    addresses: Map<String, String>,
    assets: List<AssetItem>,
    rpcRegistry: EvmRpcRegistry,
    priceRepository: PriceRepository,
    etherscan: EtherscanClient,
    onActivity: () -> Unit,
    onAssetsUpdate: (List<AssetItem>) -> Unit,
    onNativeBalanceUpdate: (BigInteger) -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
) {
    var totalUsd by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(evmAddress) {
        if (evmAddress.isBlank()) return@LaunchedEffect
        val initial = buildAssetList(addresses)
        onAssetsUpdate(initial)

        val result = loadEvmBalances(
            initial,
            evmAddress,
            rpcRegistry,
            priceRepository,
            System.currentTimeMillis(),
        )
        onAssetsUpdate(result.assets)
        onNativeBalanceUpdate(result.ethBalanceWei)
        totalUsd = result.totalUsd

        // Токены догружаются вторым шагом: монеты уже видны, ожидание Etherscan их не задерживает.
        val tokens = loadTokens(evmAddress, rpcRegistry, etherscan)
        if (tokens.isNotEmpty()) onAssetsUpdate(result.assets + tokens)
    }

    HomeScreen(
        assets = assets.ifEmpty { buildAssetList(addresses) },
        onSend = onSend,
        onReceive = onReceive,
        onActivity = onActivity,
        totalUsd = totalUsd,
        query = query,
        onQueryChange = { query = it },
    )
}

private fun deriveAllAddresses(seed: SensitiveBytes): Map<String, String> = mapOf(
    "Ethereum" to AccountDeriver.deriveAddress(seed, EvmAddressDeriver()),
    "Bitcoin (Segwit)" to AccountDeriver.deriveAddress(seed, BitcoinSegwitAddressDeriver()),
    "Bitcoin (Taproot)" to AccountDeriver.deriveAddress(seed, BitcoinTaprootAddressDeriver()),
    "Solana" to AccountDeriver.deriveAddress(seed, SolanaAddressDeriver()),
    "TON" to AccountDeriver.deriveAddress(seed, TonAddressDeriver()),
)

private suspend fun estimateGas(
    rpc: EvmRpcClient,
    from: String,
    to: String,
    amount: String,
    asset: SendAsset,
): GasEstimate {
    val (callTo, callValue, callData) = if (asset.isNative) {
        Triple(to, toWei(amount, asset.decimals), "0x")
    } else {
        val data = "0x" + Erc20.encodeTransfer(to, toWei(amount, asset.decimals)).toHex()
        Triple(asset.contractAddress!!, BigInteger.ZERO, data)
    }
    val gasLimit = rpc.estimateGas(from = from, to = callTo, value = callValue, data = callData)
    val baseFee = rpc.getBaseFeePerGas()
    val priorityFee = try {
        rpc.getMaxPriorityFeePerGas()
    } catch (_: Exception) {
        BigInteger.valueOf(1_500_000_000L)
    }
    return GasEstimate(
        gasLimit = gasLimit,
        maxFeePerGas = baseFee * BigInteger.valueOf(2) + priorityFee,
        maxPriorityFeePerGas = priorityFee,
    )
}

@Suppress("LongParameterList")
private suspend fun sendEvmTransaction(
    rpc: EvmRpcClient,
    mnemonicWords: List<String>,
    to: String,
    amount: String,
    gas: GasEstimate,
    asset: SendAsset,
): String {
    val seed = Mnemonic.toSeed(mnemonicWords)
    val key = HdKeyDeriver.deriveBip32(seed, "m/44'/60'/0'/0/0")
    seed.close()

    val from = EvmAddressDeriver().deriveAddress(key.publicKey)
    val nonce = rpc.getTransactionCount(from)
    val chainId = rpc.getChainId()

    val (txTo, txValue, txData) = if (asset.isNative) {
        Triple(to, toWei(amount, asset.decimals), ByteArray(0))
    } else {
        Triple(
            asset.contractAddress!!,
            BigInteger.ZERO,
            Erc20.encodeTransfer(to, toWei(amount, asset.decimals)),
        )
    }

    val tx = EvmTransaction(
        chainId = chainId,
        nonce = nonce,
        maxPriorityFeePerGas = gas.maxPriorityFeePerGas,
        maxFeePerGas = gas.maxFeePerGas,
        gasLimit = gas.gasLimit,
        to = txTo,
        value = txValue,
        data = txData,
    )

    val signedHex = key.privateKey.use { privBytes -> EvmSigner.signToHex(privBytes, tx) }
    key.close()
    return rpc.sendRawTransaction(signedHex)
}

private fun toWei(amount: String, decimals: Int): BigInteger =
    BigDecimal(amount).multiply(BigDecimal.TEN.pow(decimals)).toBigInteger()
