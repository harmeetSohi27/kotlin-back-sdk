package dev.tmsoft.lib.event

import com.google.inject.Injector
import kotlin.reflect.KClass

class GuiceSubscriberFactory(val injector: Injector) {
    inline fun <reified TEvent : Event, TSubscriber : EventSubscriber<TEvent>> get(clazz: KClass<TSubscriber>): TSubscriber {
        return injector.getInstance(clazz.java)
    }

    fun <TSubscriber : EventsSubscriber> get(clazz: KClass<TSubscriber>): TSubscriber {
        return injector.getInstance(clazz.java)
    }
}
