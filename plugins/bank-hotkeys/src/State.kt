package com.kaipov.plugins

enum class State {
    UNKNOWN,
    WAITING,
    CHOPPING,
    BRAZING,
    BRAZING_INTENSIFIES,
    END,
    ;

    override fun toString(): String {
        return name.replace("_", " ").toLowerCase()
    }
}
