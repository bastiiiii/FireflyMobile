package xyz.hisname.fireflyiii.workers.account

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import xyz.hisname.fireflyiii.Constants
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.data.local.dao.AppDatabase
import xyz.hisname.fireflyiii.data.local.pref.AppPref
import xyz.hisname.fireflyiii.data.remote.firefly.api.AccountsService
import xyz.hisname.fireflyiii.repository.models.accounts.AccountAttributes
import xyz.hisname.fireflyiii.repository.models.accounts.AccountData
import xyz.hisname.fireflyiii.repository.models.error.ErrorModel
import xyz.hisname.fireflyiii.workers.BaseWorker
import xyz.hisname.fireflyiii.ui.notifications.displayNotification
import xyz.hisname.fireflyiii.util.network.retrofitCallback
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

class AccountWorker(private val context: Context, workerParameters: WorkerParameters): BaseWorker(context, workerParameters)  {

    private val channelIcon = R.drawable.ic_euro_sign

    companion object {

        fun initWorker(context: Context, accountName: String, accountType: String,
                       currencyCode: String?, iban: String?, bic: String?, accountNumber: String?,
                       openingBalance: String?, openingBalanceDate: String?, accountRole: String?,
                       virtualBalance: String?, includeInNetWorth: Boolean, notes: String?, liabilityType: String?,
                       liabilityAmount: String?, liabilityStartDate: String?, interest: String?, interestPeriod: String?){
            val accountData = Data.Builder()
                    .putString("name", accountName)
                    .putString("type", accountType)
                    .putString("currencyCode", currencyCode)
                    .putString("iban", iban)
                    .putString("bic", bic)
                    .putString("accountNumber", accountNumber)
                    .putString("openingBalance", openingBalance)
                    .putString("openingBalanceDate", openingBalanceDate)
                    .putString("virtualBalance", virtualBalance)
                    .putBoolean("includeNetWorth", includeInNetWorth)
                    .putString("accountRole", accountRole)
                    .putString("notes", notes)
                    .putString("liabilityType", liabilityType)
                    .putString("liabilityAmount", liabilityAmount)
                    .putString("liabilityStartDate", liabilityStartDate)
                    .putString("interest", interest)
                    .putString("interestPeriod", interestPeriod)
                    .build()
            val accountTag =
                    WorkManager.getInstance(context).getWorkInfosByTag(
                            "add_periodic_account_$accountName" + "_" + accountType).get()
            if(accountTag == null || accountTag.size == 0){
                val appPref = AppPref(PreferenceManager.getDefaultSharedPreferences(context))
                val delay = appPref.workManagerDelay
                val battery = appPref.workManagerLowBattery
                val networkType = appPref.workManagerNetworkType
                val requireCharging = appPref.workManagerRequireCharging
                val accountWork = PeriodicWorkRequestBuilder<AccountWorker>(Duration.ofMinutes(delay))
                        .setInputData(accountData)
                        .setConstraints(Constraints.Builder()
                                .setRequiredNetworkType(networkType)
                                .setRequiresBatteryNotLow(battery)
                                .setRequiresCharging(requireCharging)
                                .build())
                        .addTag("add_periodic_account_$accountName" + "_" + accountType)
                        .build()
                WorkManager.getInstance(context).enqueue(accountWork)
                runBlocking(Dispatchers.IO) {
                    val accountDatabase = AppDatabase.getInstance(context).accountDataDao()
                    val currencyDatabase = AppDatabase.getInstance(context).currencyDataDao()
                    val currencyData =
                                currencyDatabase.getCurrencyByCode(currencyCode ?: "")[0]
                    val currencySymbol = currencyData.currencyAttributes?.symbol
                    val currencyId = currencyData.currencyId
                    val fakeAccountId = ThreadLocalRandom.current().nextLong()
                    accountDatabase.insert(AccountData(
                            "", fakeAccountId, AccountAttributes(
                            "","", accountName, true,
                            accountType, accountRole, currencyId, currencyCode, 0.0,
                            currencySymbol, "", notes, "", "", accountNumber,
                            iban, bic, 0.0,
                            openingBalance, openingBalanceDate, liabilityType,
                            liabilityAmount, liabilityStartDate, interest, interestPeriod, includeInNetWorth, true)
                    ))
                }
            }
        }

        fun cancelWorker(accountName: String, accountType: String, context: Context){
            runBlocking(Dispatchers.IO) {
                val accountDatabase = AppDatabase.getInstance(context).accountDataDao()
                accountDatabase.deleteAccountByTypeAndName(accountType, accountName)
                accountDatabase
            }
            WorkManager.getInstance(context).cancelAllWorkByTag("add_periodic_account_$accountName" + "_" + accountType)
        }

    }

    override suspend fun doWork(): Result {
        val name = inputData.getString("name") ?: ""
        val accountType = inputData.getString("type") ?: ""
        val currencyCode = inputData.getString("currencyCode") ?: ""
        val includeNetWorth = inputData.getBoolean("includeNetWorth",false)
        val accountRole = inputData.getString("accountRole")
        val liabilityType = inputData.getString("liabilityType")
        val liabilityAmount = inputData.getString("liabilityAmount")
        val liabilityStartDate = inputData.getString("liabilityStartDate")
        val interest = inputData.getString("interest")
        val interestPeriod = inputData.getString("interestPeriod")
        val accountNumber = inputData.getString("accountNumber")
        val iBanString = inputData.getString("iban")
        val bicString = inputData.getString("bic")
        val openingBalance = inputData.getString("openingBalance")
        val openingBalanceDate = inputData.getString("openingBalanceDate")
        val virtualBalance = inputData.getString("virtualBalance")
        val notes = inputData.getString("notes")
        genericService?.create(AccountsService::class.java)?.addAccount(name, accountType, currencyCode,
                iBanString, bicString, accountNumber, openingBalance, openingBalanceDate,
                accountRole, virtualBalance, includeNetWorth, notes, liabilityType, liabilityAmount,
                liabilityStartDate, interest, interestPeriod)?.enqueue(
                retrofitCallback({ response ->
                    var errorBody = ""
                    var error = ""
                    if (response.errorBody() != null) {
                        errorBody = String(response.errorBody()?.bytes()!!)
                    }
                    val gson = Gson().fromJson(errorBody, ErrorModel::class.java)
                    val responseBody = response.body()
                    if (response.isSuccessful && responseBody != null) {
                        context.displayNotification("$name was added successfully!", "Account Added",
                                Constants.ACCOUNT_CHANNEL, channelIcon)
                        cancelWorker(name,accountType, context)
                        runBlocking(Dispatchers.IO){
                            val accountDatabase = AppDatabase.getInstance(context).accountDataDao()
                            accountDatabase.insert(responseBody.data)
                        }
                        Result.success()
                    } else {
                        error = when {
                            gson.errors.name != null -> gson.errors.name[0]
                            gson.errors.account_number != null -> gson.errors.account_number[0]
                            gson.errors.interest != null -> gson.errors.interest[0]
                            gson.errors.liabilityStartDate != null -> gson.errors.liabilityStartDate[0]
                            else -> "Error saving account"
                        }
                        cancelWorker(name,accountType, context)
                        context.displayNotification(error, "There was an error adding $name",
                                Constants.ACCOUNT_CHANNEL, channelIcon)
                        Result.failure()
                    }
                })
                { throwable ->
                    Result.retry()
                })
        return Result.success()
    }
}