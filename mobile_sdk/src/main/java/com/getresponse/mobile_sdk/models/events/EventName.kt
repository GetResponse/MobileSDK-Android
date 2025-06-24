package com.getresponse.mobile_sdk.models.events


enum class EventName(val value: String) {
    VIEW_ITEM("view_item"),
    VIEW_CATEGORY("view_category"),
    WISHLIST_ITEM("wishlist_item"),
    LIKE_ITEM("like_item"),
    UNLIKE_ITEM("unlike_item"),
    ORDER_PLACED("order_placed"),
    ORDER_PAID("order_paid"),
    CART_UPDATE("cart_update")
}