/*
 * Copyright 2023, R3 LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.r3.corda.interop.evm.common.trie

/**
 * Convert a ByteArray to nibbles.
 *
 * Each byte is divided into two nibbles, high nibble first.
 *
 * @return ByteArray of nibbles.
 */
fun ByteArray.toNibbles(): ByteArray {
    val result = ByteArray(this.size * 2)
    for (i in this.indices) {
        result[i * 2] = ((this[i].toInt() shr 4) and 0x0F).toByte()
        result[i * 2 + 1] = (this[i].toInt() and 0x0F).toByte()
    }
    return result
}

/**
 * Checks if the ByteArray starts with the specified ByteArray.
 *
 * The function iterates over the elements in the original ByteArray and
 * the other ByteArray simultaneously. If the original ByteArray is shorter
 * than the other ByteArray, or if any element in the original ByteArray
 * doesn't match the corresponding element in the other ByteArray, the
 * function returns false. If all elements match, the function returns true.
 *
 * @param other the ByteArray to check at the start of this ByteArray
 * @return true if this ByteArray starts with the specified ByteArray, false otherwise
 */
fun ByteArray.startsWith(other: ByteArray): Boolean {
    if (other.size > this.size) {
        return false
    }
    for (i in other.indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true
}
