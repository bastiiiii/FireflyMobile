package xyz.hisname.fireflyiii.util.extension

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import xyz.hisname.fireflyiii.util.Sixple

// Fragment's View Model should not hold references to activity unless necessary(communication between 2 fragments)
internal fun <T: ViewModel> Fragment.getViewModel(modelClass: Class<T>, viewModelFactory: ViewModelProvider.Factory? = null): T {
    return viewModelFactory?.let { ViewModelProvider(requireActivity(), it).get(modelClass) } ?:
    ViewModelProvider(requireActivity()).get(modelClass)
}

internal fun <T: ViewModel> AppCompatActivity.getViewModel(modelClass: Class<T>, viewModelFactory: ViewModelProvider.Factory? = null): T{
    return viewModelFactory?.let { ViewModelProvider(this, it).get(modelClass) } ?:
    ViewModelProvider(this).get(modelClass)
}

internal fun <T: ViewModel> Fragment.getImprovedViewModel(modelClass: Class<T>, viewModelFactory: ViewModelProvider.Factory? = null): T {
    return viewModelFactory?.let { ViewModelProvider(this, it).get(modelClass) } ?:
    ViewModelProvider(this).get(modelClass)
}


/*
 * Copyright (C) 2017 Mitchell Skaggs, Keturah Gadson, Ethan Holtgrieve, Nathan Skelton, Pattonville School District
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
// Taken from: https://github.com/magneticflux-/kotlin-livedata-utils/blob/master/kotlin-livedata-utils/src/main/java/com/github/magneticflux/livedata/LiveDataUtils.kt
fun <A, B> zipLiveData(a: LiveData<A>, b: LiveData<B>): LiveData<Pair<A, B>> {
    return MediatorLiveData<Pair<A, B>>().apply {
        var lastA: A? = null
        var lastB: B? = null

        fun update() {
            val localLastA = lastA
            val localLastB = lastB
            if (localLastA != null && localLastB != null)
                this.value = Pair(localLastA, localLastB)
        }

        addSource(a) {
            lastA = it
            update()
        }
        addSource(b) {
            lastB = it
            update()
        }
    }
}

fun <A, B, C, D, E, F> zipLiveData(a: LiveData<A>, b: LiveData<B>, c: LiveData<C>, d: LiveData<D>, e: LiveData<E>,
                                   f: LiveData<F>): LiveData<Sixple<A, B, C, D, E, F>> {
    return MediatorLiveData<Sixple<A, B, C, D, E, F>>().apply {
        var lastA: A? = null
        var lastB: B? = null
        var lastC: C? = null
        var lastD: D? = null
        var lastE: E? = null
        var lastF: F? = null

        fun update() {
            val localLastA = lastA
            val localLastB = lastB
            val localLastC = lastC
            val localLastD = lastD
            val localLastE = lastE
            val localLastF = lastF
            if (localLastA != null && localLastB != null && localLastC != null && localLastD != null &&
                    localLastE != null && localLastF != null)
                this.value = Sixple(localLastA, localLastB, localLastC, localLastD, localLastE, localLastF)
        }

        addSource(a) {
            lastA = it
            update()
        }
        addSource(b) {
            lastB = it
            update()
        }
        addSource(c){
            lastC = it
            update()
        }
        addSource(d){
            lastD = it
            update()
        }
        addSource(e){
            lastE = it
            update()
        }
        addSource(f){
            lastF = it
            update()
        }
    }
}
