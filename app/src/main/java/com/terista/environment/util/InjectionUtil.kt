package com.terista.environment.util

import com.terista.environment.data.AppsRepository
import com.terista.environment.data.FakeLocationRepository
import com.terista.environment.data.GmsRepository

import com.terista.environment.view.apps.AppsFactory
import com.terista.environment.view.fake.FakeLocationFactory
import com.terista.environment.view.gms.GmsFactory
import com.terista.environment.view.list.ListFactory



object InjectionUtil {

    private val appsRepository = AppsRepository()



    private val gmsRepository = GmsRepository()

    private val fakeLocationRepository = FakeLocationRepository()

    fun getAppsFactory() : AppsFactory {
        return AppsFactory(appsRepository)
    }

    fun getListFactory(): ListFactory {
        return ListFactory(appsRepository)
    }


    fun getGmsFactory():GmsFactory{
        return GmsFactory(gmsRepository)
    }

    fun getFakeLocationFactory():FakeLocationFactory{
        return FakeLocationFactory(fakeLocationRepository)
    }
}