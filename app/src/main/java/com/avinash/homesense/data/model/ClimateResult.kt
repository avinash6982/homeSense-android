package com.avinash.homesense.data.model

sealed interface ClimateResult<out T> {
    data class Success<T>(val data: T) : ClimateResult<T>
    data class Failure(val error: ClimateError) : ClimateResult<Nothing>
}

enum class ClimateError {
    NO_INTERNET,
    API_FAILURE,
    EMPTY,
}
