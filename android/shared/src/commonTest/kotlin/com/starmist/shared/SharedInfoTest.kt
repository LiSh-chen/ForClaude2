package com.starmist.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class SharedInfoTest {
    @Test fun nameIsStable() = assertEquals("StarMist shared", SharedInfo.NAME)
}
