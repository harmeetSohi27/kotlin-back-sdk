package dev.tmsoft.lib.config.hoplite

import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.PropertySourceContext
import com.sksamuel.hoplite.fp.valid
import com.sksamuel.hoplite.parsers.toNode
import java.util.Properties

class EnvironmentVariablesPropertySource(
    private val useUnderscoresAsSeparator: Boolean,
    private val allowUppercaseNames: Boolean
) : PropertySource {
    override fun node(context: PropertySourceContext): ConfigResult<Node> {
        val props = Properties()
        System.getenv().forEach {
            val key = it.key
                .let { key -> if (useUnderscoresAsSeparator) key.replace("_", ".") else key }
                .let { key ->
                    if (allowUppercaseNames && Character.isUpperCase(key.codePointAt(0))) {
                        key.split(".").joinToString(separator = ".") { value ->
                            value.fold("") { acc, char ->
                                when {
                                    acc.isEmpty() -> acc + char.toLowerCase()
                                    acc.last() == '_' -> acc.dropLast(1) + char.toUpperCase()
                                    else -> acc + char.toLowerCase()
                                }
                            }
                        }
                    } else {
                        key
                    }
                }
            props[key] = it.value
        }
        return props.toNode("env").valid()
    }
}
