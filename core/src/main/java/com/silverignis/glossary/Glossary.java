package com.silverignis.glossary;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Game terms that earn an explanation wherever they appear in description
 *  text. Descriptions stay plain; {@link #markup} styles each term in place
 *  and {@link #termsIn} lists the ones a text mentions, for side cards.
 *  Any word or phrase added to {@link #TERMS} lights up everywhere. */
public final class Glossary {

    /** TextraTypist markup opened before a term; every span closes with "[ ]"
     *  (reset), which also ends any {EFFECT} an override opens. */
    // ponytail: UiTheme.GOLD's hex repeated here so this file stays libGDX-free and main() runs bare.
    // No [*]: TextraTypist 2.2.14 draws faux bold with a hardcoded 1-unit offset, unusable on the
    // world-unit stage (and the body fonts are already the Bold TTF). Weight needs a real bold face.
    private static final String DEFAULT_OPEN = "[#ffd81d]";
    private static final String CLOSE = "[ ]";

    public record Term(String name, String definition, String open) {
        public Term(String name, String definition) { this(name, definition, DEFAULT_OPEN); }
    }

    private static final List<Term> TERMS = List.of(
        // skill keywords
        new Term("Surge",      "Surge X: triggers when the card resolves as the Xth skill of a loaded burst."),
        new Term("Overheat",   "Overheat X: on cast, auto-spends X mana if you have it to trigger."),
        new Term("Cultivate",  "Cultivate X: counts your casts of this skill. From the Xth on, it is fully grown."),
        new Term("Virulence",  "Virulence X: triggers if the target already carries X or more Poison stacks."),
        new Term("Bargain",    "Bargain X: fired from a burst, auto-spends X mana to return the card to the bottom of its slot."),
        new Term("Overcharge", "Overcharge Xs: triggers if the card sat staged for X seconds before firing."),
        new Term("Chill",      "Chill X: banks X Chill stacks. Staging an ice card spends one stack per mana of discount."),
        new Term("Debt",       "Debt X: you may stage this card even if you are short by up to X mana. The missing mana is owed: your regen pays it off first, and your bar only refills after."),
        // statuses
        new Term("Poison",     "Ticks every second for 3 damage per stack, then loses a stack."),
        new Term("Burn",       "5 damage every half second. Halves healing while it lasts."),
        new Term("Stun",       "Cannot move or basic attack. Casting still works."),
        new Term("Miasma",     "Slows recovery: mana regen for you, cooldowns for an enemy."),
        new Term("Sap",        "For 3 seconds the victim leaks once a second: each leak heals the caster and, if the victim has mana, drains 1."),
        new Term("Metabolize", "Metabolize X: X stacks; each second consumes one to recover 1 mana, even at full.")
    );

    private static final Map<String, Term> BY_NAME = TERMS.stream()
        .collect(Collectors.toUnmodifiableMap(t -> t.name().toLowerCase(), Function.identity()));

    /** Whole-word, case-insensitive, longest name first; an optional trailing
     *  number ("Surge 3", "Overcharge 6s") rides along inside the match. */
    private static final Pattern PATTERN = Pattern.compile(
        "(?i)\\b(" + TERMS.stream().map(Term::name)
            .sorted(Comparator.comparingInt(String::length).reversed())
            .map(Pattern::quote).collect(Collectors.joining("|"))
        + ")(?:\\s\\d+(?:\\.\\d+)?s?)?\\b");

    private Glossary() {}

    /** Plain text → markup with every term styled. Literal brackets are escaped first. */
    public static String markup(String plain) {
        String escaped = plain.replace("[", "[[").replace("{", "{{");
        return PATTERN.matcher(escaped).replaceAll(m ->
            Matcher.quoteReplacement(term(m).open() + m.group() + CLOSE));
    }

    /** Distinct terms the text mentions, in order of first appearance. */
    public static List<Term> termsIn(String plain) {
        Set<Term> found = new LinkedHashSet<>();
        Matcher m = PATTERN.matcher(plain);
        while (m.find()) found.add(term(m));
        return List.copyOf(found);
    }

    private static Term term(MatchResult m) { return BY_NAME.get(m.group(1).toLowerCase()); }

    /** Self-check: java -ea -cp core/build/classes/java/main com.silverignis.glossary.Glossary */
    public static void main(String[] args) {
        List<Term> t = termsIn("Surge 3: x. poison y");
        assert t.size() == 2 && t.get(0).name().equals("Surge") && t.get(1).name().equals("Poison") : t;
        assert markup("Surge 3: bolt").equals("[#ffd81d]Surge 3[ ]: bolt") : markup("Surge 3: bolt");
        assert markup("Overcharge 6s: x").startsWith("[#ffd81d]Overcharge 6s[ ]") : markup("Overcharge 6s: x");
        assert termsIn("stunning saps").isEmpty();
        assert markup("[x] {y}").equals("[[x] {{y}");
        assert termsIn("Surge 3 then surge").size() == 1;
        assert termsIn("Hurl a fireball down the row.").isEmpty();
        boolean ea = false; assert ea = true;
        System.out.println(ea ? "ok" : "assertions disabled — run with -ea");
    }
}
