package com.fitness.pushuptracker.detector

/**
 * Pushup Form Validator
 * ======================
 * Implements the form scoring system with weighted components:
 * - Range of Motion: 40%
 * - Body Alignment: 30%
 * - Speed Control: 20%
 * - Arm Symmetry: 10%
 * 
 * Phase 4: Form Scoring System
 */
class PushupFormValidator {
    
    data class FormScore(
        val totalScore: Float,      // 0.0 - 1.0
        val grade: String,           // A+, A, B, C, D, F
        val rangeOfMotionScore: Float,
        val bodyAlignmentScore: Float,
        val speedControlScore: Float,
        val armSymmetryScore: Float,
        val deductions: List<String>
    )
    
    /**
     * Calculate comprehensive form score
     */
    fun calculateFormScore(
        minElbowAngle: Float,
        maxElbowAngle: Float,
        bodyAlignment: Float,
        repTime: Float,
        leftElbowAngle: Float,
        rightElbowAngle: Float
    ): FormScore {
        
        val deductions = mutableListOf<String>()
        
        // 1. Range of Motion (40% weight)
        val romScore = calculateRangeOfMotionScore(minElbowAngle, maxElbowAngle, deductions)
        
        // 2. Body Alignment (30% weight)
        val alignmentScore = calculateBodyAlignmentScore(bodyAlignment, deductions)
        
        // 3. Speed Control (20% weight)
        val speedScore = calculateSpeedControlScore(repTime, deductions)
        
        // 4. Arm Symmetry (10% weight)
        val symmetryScore = calculateArmSymmetryScore(leftElbowAngle, rightElbowAngle, deductions)
        
        // Calculate weighted total
        val totalScore = (romScore * 0.4f) + 
                        (alignmentScore * 0.3f) + 
                        (speedScore * 0.2f) + 
                        (symmetryScore * 0.1f)
        
        // Assign grade
        val grade = assignGrade(totalScore)
        
        return FormScore(
            totalScore = totalScore,
            grade = grade,
            rangeOfMotionScore = romScore,
            bodyAlignmentScore = alignmentScore,
            speedControlScore = speedScore,
            armSymmetryScore = symmetryScore,
            deductions = deductions
        )
    }
    
    /**
     * Range of Motion: 40% weight
     * Bottom <90°, Top >160°
     */
    private fun calculateRangeOfMotionScore(
        minAngle: Float,
        maxAngle: Float,
        deductions: MutableList<String>
    ): Float {
        val range = maxAngle - minAngle
        
        var score = 1.0f
        
        // Check bottom position
        if (minAngle > 100f) {
            score -= 0.3f
            deductions.add("Didn't go deep enough (${minAngle.toInt()}°)")
        } else if (minAngle > 90f) {
            score -= 0.1f
            deductions.add("Slightly shallow bottom")
        }
        
        // Check top position
        if (maxAngle < 150f) {
            score -= 0.3f
            deductions.add("Didn't fully extend (${maxAngle.toInt()}°)")
        } else if (maxAngle < 160f) {
            score -= 0.1f
            deductions.add("Slightly incomplete extension")
        }
        
        // Check overall range
        if (range < 60f) {
            score -= 0.3f
            deductions.add("Limited range of motion")
        } else if (range < 70f) {
            score -= 0.1f
        }
        
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * Body Alignment: 30% weight
     * Shoulder-Hip-Ankle should be 160°-180°
     */
    private fun calculateBodyAlignmentScore(
        alignment: Float,
        deductions: MutableList<String>
    ): Float {
        var score = 1.0f
        
        when {
            alignment < 150f -> {
                score -= 0.3f
                deductions.add("Hips sagging significantly")
            }
            alignment < 160f -> {
                score -= 0.2f
                deductions.add("Hips sagging slightly")
            }
            alignment > 185f -> {
                score -= 0.2f
                deductions.add("Hips too high")
            }
        }
        
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * Speed Control: 20% weight
     * Optimal: 1.0s - 4.0s per rep
     */
    private fun calculateSpeedControlScore(
        repTime: Float,
        deductions: MutableList<String>
    ): Float {
        var score = 1.0f
        
        when {
            repTime < 0.8f -> {
                score -= 0.4f
                deductions.add("Too fast (${String.format("%.1f", repTime)}s)")
            }
            repTime < 1.0f -> {
                score -= 0.2f
                deductions.add("Slightly fast")
            }
            repTime > 5.0f -> {
                score -= 0.4f
                deductions.add("Too slow (${String.format("%.1f", repTime)}s)")
            }
            repTime > 4.0f -> {
                score -= 0.2f
                deductions.add("Slightly slow")
            }
        }
        
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * Arm Symmetry: 10% weight
     * Left-right angle difference <20°
     */
    private fun calculateArmSymmetryScore(
        leftAngle: Float,
        rightAngle: Float,
        deductions: MutableList<String>
    ): Float {
        val difference = kotlin.math.abs(leftAngle - rightAngle)
        
        var score = 1.0f
        
        when {
            difference > 30f -> {
                score -= 0.5f
                deductions.add("Severely unbalanced arms")
            }
            difference > 20f -> {
                score -= 0.3f
                deductions.add("Unbalanced arms")
            }
            difference > 10f -> {
                score -= 0.1f
                deductions.add("Slightly unbalanced")
            }
        }
        
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * Assign letter grade based on score
     */
    private fun assignGrade(score: Float): String {
        return when {
            score >= 0.95f -> "A+"
            score >= 0.90f -> "A"
            score >= 0.85f -> "A-"
            score >= 0.80f -> "B+"
            score >= 0.75f -> "B"
            score >= 0.70f -> "B-"
            score >= 0.65f -> "C+"
            score >= 0.60f -> "C"
            score >= 0.55f -> "C-"
            score >= 0.50f -> "D"
            else -> "F"
        }
    }
}
