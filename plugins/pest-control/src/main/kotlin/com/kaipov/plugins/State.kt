package com.kaipov.plugins

enum class State {
    UNKNOWN,
    ACTING,
    ON_OUTPOST,
    ON_BOAT,
    ON_ISLAND,
    FIGHTING,
    ;

    override fun toString(): String {
        return name.replace("_", " ").toLowerCase()
    }
}

enum class BoatGangplanks(val ID: Int) {
    NOVICE(14315),
    INTERMEDIATE(25631),
    VETERAN(25632),
}
