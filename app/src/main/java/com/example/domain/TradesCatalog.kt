package com.example.domain

enum class WorkerCategory(val key: String) {
    SKILLED("SKILLED"),
    UNSKILLED("UNSKILLED")
}

object TradesCatalog {
    val skilledTrades: List<String> = listOf(
        "Electrician",
        "Plumber",
        "Carpenter",
        "Mason",
        "Painter",
        "Welder",
        "AC Technician",
        "Tiles Worker",
        "Steel Fixer",
        "Shuttering Carpenter",
        "Machine Operator",
        "Crane Operator",
        "Bar Bender",
        "Pipe Fitter",
        "Solar Technician",
        "Other Skilled Trade"
    )

    val unskilledTrades: List<String> = listOf(
        "General Labour",
        "Construction Helper",
        "Site Helper",
        "Loading Worker",
        "Cleaning Worker",
        "Warehouse Helper",
        "Packaging Worker",
        "Road Work Helper",
        "Gardening Helper",
        "Other Unskilled Category"
    )

    val companyTypes: List<String> = listOf(
        "Construction Company",
        "Infrastructure Builder",
        "Manufacturing & Factory",
        "Engineering Contractor",
        "Warehouse & Logistics",
        "Facility Management",
        "Other Employer"
    )

    val availabilityOptions: List<String> = listOf(
        "AVAILABLE",
        "WORKING_NOTICE",
        "BUSY"
    )

    fun tradesForCategory(category: String): List<String> {
        return if (category == WorkerCategory.SKILLED.key) skilledTrades else unskilledTrades
    }
}
