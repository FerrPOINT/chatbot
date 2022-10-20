package azhukov.chatbot.service.macro;

import lombok.Value;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.*;
import java.util.stream.Collectors;

public class MacrosTemplate {

    private final List<Part> parts;

    public MacrosTemplate(String text, Set<String> macrosDictionary) {
        MutableInt lastIndex = new MutableInt(0);
        List<Part> parts = new ArrayList<>();
        macrosDictionary.stream()
                .map(s -> getOccurrences(text, s))
                .flatMap(Collection::stream)
                .sorted(Comparator.comparingInt(Occurrence::getIndex))
                .map(occurrence -> {
                    int initial = lastIndex.intValue();
                    lastIndex.setValue(occurrence.getIndex() + occurrence.getMacro().length());
                    return occurrence.getIndex() == 0 ?
                            List.of(
                                    new Part(text.substring(occurrence.getIndex(), lastIndex.getValue()), true)
                            ) :
                            List.of(
                                    new Part(text.substring(initial, occurrence.getIndex()), false),
                                    new Part(text.substring(occurrence.getIndex(), lastIndex.getValue()), true)
                            );
                })
                .flatMap(Collection::stream)
                .forEach(parts::add);

        if (parts.isEmpty()) {
            this.parts = List.of(new Part(text, false));
        } else {
            Part part = parts.get(parts.size() - 1);
            if (part.isMacro() && lastIndex.intValue() < text.length()) {
                parts.add(new Part(text.substring(lastIndex.intValue()), false));
            }
            this.parts = parts;
        }
    }

    public String compileString(Map<String, String> macroToValue) {
        return parts.stream()
                .map(part -> part.isMacro() ? macroToValue.get(part.getValue()) : part.getValue())
                .collect(Collectors.joining(""));
    }

    private List<Occurrence> getOccurrences(String text, String macro) {
        List<Occurrence> ocs = new ArrayList<>();
        int index = text.indexOf(macro);
        while (index >= 0) {
            ocs.add(new Occurrence(macro, index));
            index = text.indexOf(macro, index + 1);
        }
        return ocs;
    }

    @Value
    private static class Occurrence {
        String macro;
        int index;
    }

    @Value
    private static class Part {
        String value;
        boolean macro;
    }

}
