package xyz.hisname.fireflyiii.workers.bill

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.*
import xyz.hisname.fireflyiii.data.remote.firefly.api.BillsService
import xyz.hisname.fireflyiii.data.local.dao.AppDatabase
import xyz.hisname.fireflyiii.data.local.pref.AppPref
import xyz.hisname.fireflyiii.repository.bills.BillRepository
import xyz.hisname.fireflyiii.util.network.HttpConstants
import xyz.hisname.fireflyiii.workers.BaseWorker
import java.time.Duration
import java.util.concurrent.TimeUnit

class DeleteBillWorker(private val context: Context, workerParameters: WorkerParameters): BaseWorker(context, workerParameters) {

    private val billDatabase by lazy { AppDatabase.getInstance(context).billDataDao() }

    companion object {
        fun initPeriodicWorker(billId: Long, context: Context){
            val billTag =
                    WorkManager.getInstance(context).getWorkInfosByTag("delete_bill_periodic_$billId").get()
            if(billTag == null || billTag.size == 0) {
                val billData = Data.Builder()
                        .putLong("billId", billId)
                        .build()
                val appPref = AppPref(PreferenceManager.getDefaultSharedPreferences(context))
                val delay = appPref.workManagerDelay
                val battery = appPref.workManagerLowBattery
                val networkType = appPref.workManagerNetworkType
                val requireCharging = appPref.workManagerRequireCharging
                val deleteBillWork = PeriodicWorkRequestBuilder<DeleteBillWorker>(Duration.ofMinutes(delay))
                        .setInputData(billData)
                        .addTag("delete_bill_periodic_$billId")
                        .setConstraints(Constraints.Builder()
                                .setRequiredNetworkType(networkType)
                                .setRequiresBatteryNotLow(battery)
                                .setRequiresCharging(requireCharging)
                                .build())
                        .build()
                WorkManager.getInstance(context).enqueue(deleteBillWork)
            }
        }

        fun cancelWorker(billId: Long, context: Context){
            WorkManager.getInstance(context).cancelAllWorkByTag("delete_bill_periodic_$billId")
        }
    }

    override suspend fun doWork(): Result {
        val billId = inputData.getLong("billId", 0)
        val billService = genericService?.create(BillsService::class.java)
        val repository = BillRepository(billDatabase, billService)
        return when(repository.deleteBillById(billId)){
            HttpConstants.NO_CONTENT_SUCCESS -> {
                cancelWorker(billId, context)
                Result.success()
            }
            HttpConstants.FAILED -> {
                Result.retry()
            }
            else -> {
                Result.failure()
            }
        }
    }


}