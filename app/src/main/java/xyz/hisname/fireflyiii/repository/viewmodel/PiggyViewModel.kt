package xyz.hisname.fireflyiii.repository.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.*
import xyz.hisname.fireflyiii.repository.RetrofitBuilder
import xyz.hisname.fireflyiii.repository.api.PiggybankService
import xyz.hisname.fireflyiii.repository.dao.AppDatabase
import xyz.hisname.fireflyiii.repository.models.piggy.PiggyApiResponse
import xyz.hisname.fireflyiii.repository.models.piggy.PiggyData
import xyz.hisname.fireflyiii.util.retrofitCallback

class PiggyBankViewModel(application: Application) : AndroidViewModel(application) {

    private val piggyDataBase by lazy { AppDatabase.getInstance(application)?.piggyDataDao() }
    private var piggyBankService: PiggybankService? = null

    fun getPiggyBank(baseUrl: String?, accessToken: String?): PiggyResponse{
        val apiLiveData: MutableLiveData<PiggyApiResponse> = MutableLiveData()
        val apiResponse: MediatorLiveData<PiggyApiResponse> =  MediatorLiveData()
        piggyBankService = RetrofitBuilder.getClient(baseUrl,accessToken)?.create(PiggybankService::class.java)
        piggyBankService?.getPiggyBanks()?.enqueue(retrofitCallback({ response ->
            if (response.isSuccessful) {
                response.body()?.data?.forEachIndexed { _, element ->
                    GlobalScope.launch(Dispatchers.Default, CoroutineStart.DEFAULT, null, {
                        piggyDataBase?.addPiggy(element)
                    })
                }
            } else {
                var errorBody = ""
                if (response.errorBody() != null) {
                    errorBody = String(response.errorBody()?.bytes()!!)
                }
                apiLiveData.postValue(PiggyApiResponse(errorBody))
            }
        })
        { throwable ->  apiLiveData.value = PiggyApiResponse(throwable)})

        apiResponse.addSource(apiLiveData) { apiResponse.value = it }
        return PiggyResponse(piggyDataBase?.getPiggy(), apiResponse)
    }

    fun deletePiggyBank(baseUrl: String?, accessToken: String?,id: String): LiveData<PiggyApiResponse>{
        val apiLiveData: MutableLiveData<PiggyApiResponse> = MutableLiveData()
        val apiResponse: MediatorLiveData<PiggyApiResponse> =  MediatorLiveData()
        piggyBankService = RetrofitBuilder.getClient(baseUrl,accessToken)?.create(PiggybankService::class.java)
        piggyBankService?.deletePiggyBankById(id)?.enqueue(retrofitCallback({ response ->
            if (response.isSuccessful) {
                GlobalScope.launch(Dispatchers.Default, CoroutineStart.DEFAULT, null, {
                    piggyDataBase?.deletePiggyById(id.toLong())
                })
            } else {
                var errorBody = ""
                if (response.errorBody() != null) {
                    errorBody = String(response.errorBody()?.bytes()!!)
                }
                apiLiveData.postValue(PiggyApiResponse(errorBody))
            }
        })
        { throwable -> apiLiveData.value = PiggyApiResponse(throwable)})
        apiResponse.addSource(apiLiveData){ apiResponse.value = it }
        return apiResponse
    }

    fun addPiggyBank(baseUrl: String?, accessToken: String?, piggyName: String, accountId: String,
                     currentAmount: String?, notes: String?, startDate: String?, targetAmount: String,
                     targetDate: String?): LiveData<PiggyApiResponse>{
        val apiResponse: MediatorLiveData<PiggyApiResponse> =  MediatorLiveData()
        val piggy: MutableLiveData<PiggyApiResponse> = MutableLiveData()
        piggyBankService = RetrofitBuilder.getClient(baseUrl,accessToken)?.create(PiggybankService::class.java)
        piggyBankService?.createNewPiggyBank(piggyName, accountId, targetAmount, currentAmount, startDate,
                targetDate, notes)?.enqueue(retrofitCallback({ response ->
            var errorBody = ""
            if (response.errorBody() != null) {
                errorBody = String(response.errorBody()?.bytes()!!)
            }
            if(response.isSuccessful){
                piggy.postValue(PiggyApiResponse(response.body()))
            } else {
                piggy.postValue(PiggyApiResponse(errorBody))
            }
        })
        { throwable ->
            apiResponse.postValue(PiggyApiResponse(throwable))
        })
        apiResponse.addSource(piggy){ apiResponse.value = it }
        return apiResponse

    }

}

data class PiggyResponse(val databaseData: LiveData<MutableList<PiggyData>>?, val apiResponse: MediatorLiveData<PiggyApiResponse>)