package wex.wallet.data.activity

import wex.wallet.core.network.TokenTransfer
import wex.wallet.data.portfolio.SpamFilter
import wex.wallet.data.portfolio.TokenBalance
import java.math.BigInteger

/**
 * Переиспользует текстовые правила [SpamFilter] для переводов.
 *
 * Нюанс: SpamFilter считает спамом нулевой *баланс*, но нулевая *сумма перевода*
 * спамом не является — так выглядят обычные вызовы контрактов. Поэтому сюда
 * подставляется ненулевая сумма, и срабатывают только правила по тексту и длине.
 */
internal object ActivitySpamRules {

    fun isSpam(transfer: TokenTransfer): Boolean = SpamFilter.isSpam(
        TokenBalance(
            contractAddress = transfer.contractAddress,
            symbol = transfer.tokenSymbol,
            name = transfer.tokenName,
            decimals = transfer.tokenDecimals,
            balance = transfer.value.max(BigInteger.ONE),
        ),
    )
}
