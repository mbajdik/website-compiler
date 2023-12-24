/*
 * Copyright (C) 2023 Bajdik Márton
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package me.mbajdik.webcompiler.compiler.processor

import me.mbajdik.webcompiler.state.data.ErrorMessage
import me.mbajdik.webcompiler.task.tasks.CSSProcessTask

object CSS {
    fun process(task: CSSProcessTask): CompilerOutput {
        val output = StringBuilder();
        val embedded = mutableListOf<String>();

        var status = InterpreterStatus.OUTSIDE;
        var string = StringType.SINGLE_QUOTE;

        var inImport = false;
        var importStartIndex = -1; // used in earlier versions, might need later
        var importFoundToken = false;
        var importInParentheses = false;
        var importFileBuilder = StringBuilder()
        var importBackupBuilder = StringBuilder()

        val importFiles = mutableListOf<String>();

        val input = task.handler.fileContentsString(task.manager)
        val chars = input.toCharArray();
        val l = chars.size;

        var skip = 0;
        for (i in 0 until l) {
            if (skip > 0) {
                skip--;
                continue;
            }

            val c = chars[i];

            if (status == InterpreterStatus.OUTSIDE)  {
                when (c) {
                    '\'' -> {
                        status = InterpreterStatus.STRING
                        string = StringType.SINGLE_QUOTE
                        if (!inImport) output.append("'")
                        continue
                    }
                    '"' -> {
                        status = InterpreterStatus.STRING
                        string = StringType.DOUBLE_QUOTE
                        if (!inImport) output.append("\"")
                        continue
                    }
                    '/' -> {
                        if (i + 1 < l && chars[i + 1] == '*') {
                            status = InterpreterStatus.COMMENT
                            output.append("/*")
                            skip = 1
                            continue
                        }
                    }
                }
            }

            if (status == InterpreterStatus.COMMENT) {
                if (c == '*' && i + 1 < l && chars[i + 1] == '/') {
                    status = InterpreterStatus.OUTSIDE
                    output.append("*/")
                    skip = 1
                    continue
                }
            }

            if (status == InterpreterStatus.STRING) {
                if (c == string.char && chars[i - 1] != '\\') {
                    status = InterpreterStatus.OUTSIDE
                    if (!inImport) {
                        output.append(c)
                        continue
                    }
                }
                if (inImport) {
                    importFileBuilder.append(c)
                    continue
                }
                when (c) {
                    '\n' -> output.append("\\n")
                    '\r' -> output.append("\\r")
                    else -> output.append(c)
                }
            }

            if (status == InterpreterStatus.OUTSIDE) {
                if (inImport) {
                    importBackupBuilder.append(c)
                    if (!Character.isWhitespace(c) && !importFoundToken) {
                        if (c == 'u' && i + 2 < l && chars[i + 1] == 'r' && chars[i + 2] == 'l') {
                            importFoundToken = true
                            importBackupBuilder.append("rl")
                            skip = 2
                        } else {
                            task.manager.pushErrorMessage(
                                task,
                                ErrorMessage.MessageType.ERROR,
                                "@import not used with url(...)",
                                ErrorMessage.CodeSnippet.fromCode(input, i)
                            )
                            inImport = false
                            output.append(importBackupBuilder)
                        }
                        continue
                    }
                    if (importFoundToken) {
                        if (!importInParentheses && !(c == '(' || Character.isWhitespace(c))) {
                            task.manager.pushErrorMessage(
                                task,
                                ErrorMessage.MessageType.ERROR,
                                "@import url(...) pattern used without parentheses",
                                ErrorMessage.CodeSnippet.fromCode(input, i)
                            )

                            inImport = false
                            output.append(importBackupBuilder)
                            continue
                        } else if (!importInParentheses && !Character.isWhitespace(c)) {
                            importInParentheses = true
                            continue
                        }
                        if (c == ')') {
                            inImport = false
                            importFiles.add(importFileBuilder.toString())

                            val subtask = task.subtaskCSS(importFileBuilder.toString());

                            val childInterpreter = subtask.process();
                            embedded.add(childInterpreter.output)
                            embedded.addAll(childInterpreter.embedded)
                            skip = 1
                            continue
                        } else {
                            importFileBuilder.append(c)
                        }
                    }
                } else {
                    // kotlin.String(Arrays.copyOfRange(chars, i, i + 8))
                    if (c == '@' && i + 7 < l && String(chars.slice(i..i+7).toCharArray()) == "@import ") {
                        // Starting import sequence
                        inImport = true
                        importStartIndex = i
                        importBackupBuilder = StringBuilder("@import ")
                        importFoundToken = false
                        importInParentheses = false
                        importFileBuilder = StringBuilder()
                        skip = 7
                        continue
                    }
                }
            }

            if ((status == InterpreterStatus.OUTSIDE && !inImport) || status == InterpreterStatus.COMMENT) {
                output.append(c)
            }
        }

        return CompilerOutput(output.toString(), embedded);
    }

    data class CompilerOutput(val output: String, val embedded: List<String>)

    enum class InterpreterStatus {OUTSIDE, STRING, COMMENT}
    enum class StringType(val char: Char) {SINGLE_QUOTE('\''), DOUBLE_QUOTE('"')}
}