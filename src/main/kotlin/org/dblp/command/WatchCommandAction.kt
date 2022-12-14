package org.dblp.command

enum class WatchCommandAction(val numberOfArguments: Int) {
    CHECK(1),
    
    REGISTER(3),

    DELETE(2),

    UPDATE(3)
}