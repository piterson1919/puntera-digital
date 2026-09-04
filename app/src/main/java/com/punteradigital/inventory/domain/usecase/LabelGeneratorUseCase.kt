package com.punteradigital.inventory.domain.usecase

import com.punteradigital.inventory.domain.model.Origin
import java.util.UUID
import javax.inject.Inject

/**
 * Generates unique identifiers following the naming convention:
 *   {PREFIX}-{LOT_SHORT}-{SIZE}-{SEQUENCE}
 *
 * Example: FS-2026A-42-001 (Foot Safe, lot 2026A, size 42, unit 1)
 * Master:  FS-MB-2026A-42  (Parent of children)
 */
class UuidGeneratorUseCase @Inject constructor() {

    /**
     * Generates a single unit UUID with the correct origin prefix.
     */
    fun generateUnitUuid(origin: Origin, lot: String, size: String, sequence: Int): String {
        val seq = sequence.toString().padStart(3, '0')
        val lotShort = lot.take(8).uppercase().replace(" ", "")
        return "${origin.prefix}-$lotShort-$size-$seq"
    }

    /**
     * Generates a Master Box UUID.
     */
    fun generateMasterBoxUuid(origin: Origin, lot: String, size: String, boxSequence: Int): String {
        val boxSeq = boxSequence.toString().padStart(2, '0')
        val lotShort = lot.take(8).uppercase().replace(" ", "")
        return "${origin.prefix}-MB-$lotShort-$size-$boxSeq"
    }

    /**
     * Generates a batch of UUIDs for a Master Box scenario:
     *   1 parent UUID + N child UUIDs
     *
     * @return Pair of (parentUuid, list of child UUIDs)
     */
    fun generateMasterBoxBatch(
        origin: Origin,
        lot: String,
        size: String,
        childCount: Int,
        boxSequence: Int,
        startSequence: Int
    ): Pair<String, List<String>> {
        val parentUuid = generateMasterBoxUuid(origin, lot, size, boxSequence)
        val children = (0 until childCount).map { i ->
            generateUnitUuid(origin, lot, size, startSequence + i)
        }
        return parentUuid to children
    }

    /**
     * Generates UUIDs for individual units (no master box).
     */
    fun generateUnitBatch(
        origin: Origin,
        lot: String,
        size: String,
        count: Int,
        startSequence: Int = 1
    ): List<String> {
        return (0 until count).map { i ->
            generateUnitUuid(origin, lot, size, startSequence + i)
        }
    }
}
