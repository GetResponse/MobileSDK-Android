package com.getresponse.mobile_sdk.events

import android.util.Log
import com.getresponse.mobile_sdk.GetResponseMobileSdkClient.getApi
import com.getresponse.mobile_sdk.models.User
import com.getresponse.mobile_sdk.models.events.AppInfo
import com.getresponse.mobile_sdk.models.events.Category
import com.getresponse.mobile_sdk.models.events.Event
import com.getresponse.mobile_sdk.models.events.event_data.EventData
import com.getresponse.mobile_sdk.models.events.EventName
import com.getresponse.mobile_sdk.models.events.EventPayload
import com.getresponse.mobile_sdk.models.events.OrderProduct
import com.getresponse.mobile_sdk.models.events.Product
import com.getresponse.mobile_sdk.models.events.Shop
import com.getresponse.mobile_sdk.models.events.Visitor
import com.getresponse.mobile_sdk.models.events.event_data.EventCartData
import com.getresponse.mobile_sdk.models.events.event_data.EventCategoryData
import com.getresponse.mobile_sdk.models.events.event_data.EventItemData
import com.getresponse.mobile_sdk.models.events.event_data.EventOrderData
import java.util.Calendar
import java.util.TimeZone

class GetResponseEventsService(
    private var isAvailable: Boolean = false,
    private var isEnabled: Boolean = false
) {
    private lateinit var shop: Shop
    private lateinit var user: User
    private lateinit var endpoint: String
    private lateinit var installationUUID: String
    private var enableDebug: Boolean = false
    val events: MutableList<EventPayload<EventData>> = mutableListOf()

    private fun appInfo(lang: String): AppInfo {
        return AppInfo("Android", installationUUID, lang)
    }

    internal fun configure(shop: Shop, user: User, endpoint: String, installationUUID: String, enableDebug: Boolean, isAvailable: Boolean) {
        this.shop = shop
        this.user = user
        this.endpoint = endpoint
        this.installationUUID = installationUUID
        this.enableDebug = enableDebug
        this.isAvailable = isAvailable
        isEnabled = true
    }

    private val nowUtc: String
        get() = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time.toInstant().toString()

    private fun addItemEvent(version: String, eventName: EventName, language: String, product: Product, categories: List<Category>) {
        val event = EventPayload(
            occurredOn = nowUtc,
            app = appInfo(language),
            userUUID = user.uuid,
            visitor = Visitor(installationUUID),
            event = Event(
                version = version,
                name = eventName.value,
                data = EventItemData(
                    shop = shop,
                    product = product,
                    categories = categories
                ) as EventData
            )
        )
        events.add(event)
        Log.d(TAG, "Event added: ${eventName.name}")
    }

    fun addViewItemEvent(language: String, product: Product, categories: List<Category>) {
        addItemEvent("1.0", EventName.VIEW_ITEM, language, product, categories)
    }

    fun addWishListItemEvent(language: String, product: Product, categories: List<Category>) {
        addItemEvent("1.0", EventName.WISHLIST_ITEM, language, product, categories)
    }

    fun addLikeItemEvent(language: String, product: Product, categories: List<Category>) {
        addItemEvent("1.0", EventName.LIKE_ITEM, language, product, categories)
    }

    fun addUnlikeItemEvent(language: String, product: Product, categories: List<Category>) {
        addItemEvent("1.0", EventName.UNLIKE_ITEM, language, product, categories)
    }

    fun addViewCategoryEvent(language: String, categoryId: String, categoryName: String?) {
        val event = EventPayload(
            occurredOn = nowUtc,
            app = appInfo(language),
            userUUID = user.uuid,
            visitor = Visitor(installationUUID),
            event = Event(
                version = "1.0",
                name = EventName.VIEW_CATEGORY.value,
                data = EventCategoryData(
                    shop = shop,
                    id = categoryId,
                    name = categoryName
                ) as EventData
            )
        )
        events.add(event)
        Log.d(TAG, "Event added: ${EventName.VIEW_CATEGORY.name}")
    }

    private fun addOrderEvent(
        version: String,
        eventName: EventName,
        language: String,
        orderId: String,
        currency: String,
        price: Double,
        orderProducts: List<OrderProduct>
    ) {
        val event = EventPayload(
            occurredOn = nowUtc,
            app = appInfo(language),
            userUUID = user.uuid,
            visitor = Visitor(installationUUID),
            event = Event(
                version = version,
                name = eventName.value,
                data = EventOrderData(
                    shop = shop,
                    orderId = orderId,
                    currency = currency,
                    price = price,
                    products = orderProducts
                ) as EventData
            )
        )
        events.add(event)
        Log.d(TAG, "Event added: ${eventName.name}")
    }

    private fun addCartEvent(
        version: String,
        eventName: EventName,
        language: String,
        cartUrl: String,
        currency: String,
        price: Double,
        orderProducts: List<OrderProduct>
    ) {
        val event = EventPayload(
            occurredOn = nowUtc,
            app = appInfo(language),
            userUUID = user.uuid,
            visitor = Visitor(installationUUID),
            event = Event(
                version = version,
                name = eventName.value,
                data = EventCartData(
                    cartUrl = cartUrl,
                    currency = currency,
                    price = price,
                    products = orderProducts
                ) as EventData
            )
        )
        events.add(event)
        Log.d(TAG, "Event added: ${eventName.name}")
    }

    fun addOrderPlacedEvent(language: String, orderId: String, currency: String, price: Double, orderProducts: List<OrderProduct>) {
        addOrderEvent("1.0", EventName.ORDER_PLACED, language, orderId, currency, price, orderProducts)
    }

    fun addOrderPaidEvent(language: String, orderId: String, currency: String, price: Double, orderProducts: List<OrderProduct>) {
        addOrderEvent("1.0", EventName.ORDER_PAID, language, orderId, currency, price, orderProducts)
    }

    fun addCartUpdateEvent(language: String, cartUrl: String, currency: String, price: Double, orderProducts: List<OrderProduct>) {
        addCartEvent("1.0", EventName.CART_UPDATE, language, cartUrl, currency, price, orderProducts)
    }

    suspend fun sendEvents() {
        try {
            getApi(endpoint, null, enableDebug).sendEvents(endpoint, events)
            Log.d(TAG, "${events.size} Event(s) sent successfully")
            events.clear()
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
        }
    }

    companion object {
        private const val TAG = "GetResponseMobileSDK"
    }
}