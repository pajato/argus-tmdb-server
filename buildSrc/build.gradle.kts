// SPDX-License-Identifier: LGPL-3.0-or-later

plugins {
    `kotlin-dsl`
}

kotlinDslPluginOptions { experimentalWarning.set(false) }

repositories { jcenter() }

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
