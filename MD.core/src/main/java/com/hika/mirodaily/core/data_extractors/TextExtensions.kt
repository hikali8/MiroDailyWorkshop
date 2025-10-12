package com.hika.mirodaily.core.data_extractors

import android.util.Log
import com.hika.core.aidl.accessibility.ParcelableSymbol
import com.hika.core.aidl.accessibility.ParcelableText
import com.hika.core.countSpaces

// match the first position where the sequence appears
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

// find all the positions where the sequence has appeared
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

// return the first position where one of the sequences appears first time
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

// return the first position where one of the sequences appears first time and the index of the sequence
fun ParcelableText.containsAnyWithNum(sequences: Array<String>): Pair<List<ParcelableSymbol>, Int> {
    if (this.isEmpty())
        return Pair(emptyList(), -1)
    for ((i, sequence) in sequences.withIndex()){
        if (sequence !in this.text)
            continue
        for (block in this.textBlocks){
            val text = block.text
            val index = text.indexOf(sequence)
            if (index != -1) {
                val actualIndex = index - countSpaces(text, 0, index)
                val actualLength = sequence.length - countSpaces(text, index, sequence.length)
                try {
                    return Pair(block.symbols.subList(actualIndex, actualIndex + actualLength), i)
                }catch (e: Exception){
                    Log.e("#0x-TE", "Error indexing $sequence at $actualIndex (original $index) in: $text \n actual symbols are: ${block.symbols}")
                    throw e
                }
            }
        }
    }
    return Pair(emptyList(), -1)
}