package com.hika.mirodaily.core.data_extractors

import android.util.Log
import com.hika.core.aidl.accessibility.ParcelableSymbol
import com.hika.core.aidl.accessibility.ParcelableText

fun ParcelableText.matchSequence(sequence: String): List<ParcelableSymbol> {
    if (this.isEmpty() || sequence !in this.text)
        return emptyList()
    for (block in this.textBlocks){
        val text = block.text
        val index = text.indexOf(sequence)
        if (index != -1) {
            val actualIndex = index - countSpaces(text, 0, index)
            val actualLength = sequence.length - countSpaces(text, index, sequence.length)
            try {
                return block.symbols.subList(actualIndex, actualIndex + actualLength)
            }catch (e: Exception){
                Log.e("#0x-TE", "Error indexing $sequence at $actualIndex (original $index) in: $text \n actual symbols are: ${block.symbols}")
                throw e
            }
        }
    }
    return emptyList()
}

fun ParcelableText.findAll(sequence: String): List<List<ParcelableSymbol>> {
    if (this.isEmpty() || sequence !in this.text)
        return emptyList()

    val foundSequences = mutableListOf<List<ParcelableSymbol>>()
    for (block in this.textBlocks){
        val text = block.text
        val index = text.indexOf(sequence)
        if (index != -1) {
            val actualIndex = index - countSpaces(text, 0, index)
            val actualLength = sequence.length - countSpaces(text, index, sequence.length)
            try {
                foundSequences.add(block.symbols.subList(actualIndex, actualIndex + actualLength))
            }catch (e: Exception){
                Log.e("#0x-TE", "Error indexing $sequence at $actualIndex (original $index) in: $text \n" +
                        "actual symbols are: ${block.symbols}\n")
                var str = "text is: "
                for (c in text){
                    str += c.code
                    str += ", "
                }
                Log.e("#0x-TE", str)
                throw e
            }
        }
    }
    return foundSequences
}

fun ParcelableText.containsAny(sequences: Array<String>): List<ParcelableSymbol> {
    if (this.isEmpty())
        return emptyList()
    for (sequence in sequences){
        if (sequence !in this.text)
            continue
        for (block in this.textBlocks){
            val text = block.text
            val index = text.indexOf(sequence)
            if (index != -1) {
                val actualIndex = index - countSpaces(text, 0, index)
                val actualLength = sequence.length - countSpaces(text, index, sequence.length)
                try {
                    return block.symbols.subList(actualIndex, actualIndex + actualLength)
                }catch (e: Exception){
                    Log.e("#0x-TE", "Error indexing $sequence at $actualIndex (original $index) in: $text \n actual symbols are: ${block.symbols}")
                    throw e
                }
            }
        }
    }
    return emptyList()
}

private fun countSpaces(string: String, startFrom: Int, length: Int): Int{
    var count = 0
    for (i in startFrom until startFrom + length)
        if (string[i].isWhitespace())
            count++
    return count
}