package com.focuslock.app

object Paragraphs {
    private val sentences = listOf(
        "Every minute I spend scrolling is a minute I will never get back.",
        "Discipline is choosing what I want most over what I want right now.",
        "The phone is a tool, and I decide when it gets used.",
        "Small habits, repeated every day, quietly build the person I become.",
        "Focus is not about doing more things; it is about doing the right thing well.",
        "I blocked this app for a reason, and that reason has not changed.",
        "Boredom is not an emergency, and it does not need to be fixed with a screen.",
        "Real progress happens when nobody is watching and nothing is being posted.",
        "My attention is valuable, so I will not give it away for free.",
        "Before I open this app again, I will ask myself what I am avoiding.",
        "Deep work feels slow at first, but it compounds faster than anything else.",
        "Comparison steals joy, and endless feeds are built to make me compare.",
        "The best version of today is the one where I finish what I started.",
        "I can be patient for ten more minutes; the notifications will still be there.",
        "Consistency beats intensity when the goal is to change for good.",
        "Sleep, exercise and real conversations matter more than any trending video.",
        "If this is truly important, it will still be important after I finish my work.",
        "Every time I resist the urge, the urge becomes a little weaker.",
        "Placements, projects and exams reward the hours I protect, not the hours I lose.",
        "I am building skills today that my future self will thank me for.",
        "A clear mind makes better decisions than a tired, distracted one.",
        "Typing this slowly is annoying, which is exactly why it works.",
        "Nothing on this app will matter a year from now, but my habits will.",
        "Rest is useful, but mindless scrolling is not the same thing as rest.",
        "The goal is not to never have fun; it is to stay in control of my time.",
        "Hard things become easier when I stop negotiating with myself.",
        "Each finished task is a promise kept to myself.",
        "I would rather be proud tonight than entertained for five minutes.",
        "Attention is the currency of the internet, and I am choosing to save it.",
        "When I feel the pull to check my phone, I will take one deep breath first."
    )

    /** Random paragraph with at least [minWords] words. */
    fun make(minWords: Int): String {
        val out = mutableListOf<String>()
        var words = 0
        var pool = sentences.shuffled()
        var i = 0
        while (words < minWords) {
            if (i == pool.size) { pool = sentences.shuffled(); i = 0 }
            val s = pool[i++]
            out += s
            words += s.split(" ").size
        }
        return out.joinToString(" ")
    }
}
