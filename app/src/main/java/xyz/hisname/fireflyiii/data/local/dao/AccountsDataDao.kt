package xyz.hisname.fireflyiii.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import xyz.hisname.fireflyiii.repository.models.accounts.AccountData

@Dao
abstract class AccountsDataDao: BaseDao<AccountData> {

    @Query("SELECT * FROM accounts WHERE name =:accountName")
    abstract fun getAccountByName(accountName: String): MutableList<AccountData>

    @Query("SELECT * FROM accounts WHERE type =:accountType")
    abstract fun getAccountByType(accountType: String): Flow<MutableList<AccountData>>

    @Query("SELECT * FROM accounts WHERE accountId =:accountId")
    abstract fun getAccountById(accountId: Long): MutableList<AccountData>

    @Query("DELETE FROM accounts WHERE accountId = :accountId")
    abstract fun deleteAccountById(accountId: Long): Int

    @Query("SELECT sum(current_balance) as someValue FROM accounts WHERE " +
            "currency_code =:currencyCode AND include_net_worth =:networth")
    abstract fun getAccountsWithNetworthAndCurrency(networth: Boolean = true,
                                                    currencyCode: String): Double

    @Query("DELETE FROM accounts WHERE type =:accountType AND isPending IS NOT :isPending")
    abstract fun deleteAccountByType(accountType: String, isPending: Boolean = true): Int

    @Query("DELETE FROM accounts WHERE type =:accountType AND name =:accountName")
    abstract fun deleteAccountByTypeAndName(accountType: String, accountName: String): Int

    @Query("SELECT * FROM accounts WHERE name LIKE :name AND type LIKE :type")
    abstract fun getAccountByNameAndType(type: String, name: String): Flow<MutableList<AccountData>>

}