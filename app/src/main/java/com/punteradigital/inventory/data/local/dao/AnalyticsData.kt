package com.punteradigital.inventory.data.local.dao

/**
 * Analytics data classes for traceability intelligence queries.
 */

/** Model ranked by dispatched count */
data class ModelCount(val model: String, val count: Int)

/** Size ranked by dispatched count */
data class SizeCount(val size: String, val count: Int)

/** Client ranked by order volume */
data class ClientCount(val cliente: String, val count: Int)

/** Model × Status breakdown for inventory matrix */
data class ModelStatusBreakdown(val model: String, val status: String, val count: Int)
