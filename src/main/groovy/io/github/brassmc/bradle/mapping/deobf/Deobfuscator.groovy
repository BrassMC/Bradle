/*
 * MIT License
 *
 * Copyright (c) 2022 BrassMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.brassmc.bradle.mapping.deobf

import groovy.transform.CompileStatic
import io.github.brassmc.bradle.mc.MinecraftExtension
import io.github.brassmc.bradle.util.Cacher

@CompileStatic
interface Deobfuscator {
    File binary(File input, Cacher cacher, MinecraftExtension.Mappings mappings, Side side) throws IOException
    @CompileStatic
    enum Side {
        CLIENT, SERVER
        static Side by(String name) {
            name = name.toLowerCase(Locale.ROOT)
            if (name == 'client') return CLIENT
            else if (name == 'server') return SERVER
            throw new IllegalArgumentException("Unknown side: $name")
        }
    }
}