package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.dunge.service.BossService;
import azhukov.chatbot.service.dunge.service.DungeonEconomyService;
import azhukov.chatbot.service.dunge.service.DungeonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DungeonHandler extends MessageHandler {
    private static final Set<String> DUNGEON = Set.of("!данж", "!dungeon");
    private static final Set<String> BOSS = Set.of("!босс", "!boss");
    private static final Set<String> STAT = Set.of("!стата", "!статус", "!stats");
    private static final Set<String> ARTS = Set.of("!артефакты", "!артифакты", "!artifacts");
    private static final Set<String> ABILITY = Set.of("!абилка", "!способность", "!ульт", "!абилити", "!ability");
    private final DungeonService dungeon;
    private final BossService bosses;

    @Override
    public ChatResponse answerMessage(ChatRequest request, String text, String lowerCase) {
        ParsedCommand parsed = ParsedCommand.parse(lowerCase);
        if (parsed == null) return null;
        String command = parsed.command();
        if (DUNGEON.contains(command)) return noArgs(request, parsed, () -> dungeon.getDungeonResponse(request));
        if (BOSS.contains(command)) return noArgs(request, parsed, bosses::getCurrentBossData);
        if (STAT.contains(command)) return noArgs(request, parsed, () -> dungeon.getHeroStats(request));
        if (ARTS.contains(command)) return noArgs(request, parsed, () -> dungeon.getArtifactsMessage(request));
        if (ABILITY.contains(command)) return noArgs(request, parsed, () -> dungeon.useHeroAbility(request));
        return switch (command) {
            case "!инфо", "!info" -> noArgs(request, parsed, () -> dungeon.getHeroInfo(request));
            case "!герои", "!heroes" -> noArgs(request, parsed, dungeon::getHeroesListResponse);
            case "!ладдер", "!ladder" -> noArgs(request, parsed, dungeon::getLadderResponse);
            case "!вклад", "!contribution" -> noArgs(request, parsed, dungeon::getContribution);
            case "!лечение", "!heal" -> noArgs(request, parsed, () -> action(dungeon.heal(request)));
            case "!рывок", "!rush" -> noArgs(request, parsed, () -> action(dungeon.rush(request)));
            case "!трофеи", "!trophies" -> noArgs(request, parsed, () -> dungeon.getStolenArtifacts(request));
            case "!выкуп", "!ransom" -> oneTextArg(request, parsed, id -> action(dungeon.ransom(request, id)));
            case "!осада", "!siege" -> onePositiveLong(request, parsed, amount -> action(dungeon.siege(request, amount)));
            case "!казна", "!treasury" -> parsed.args().length == 0
                    ? createMessage(request, dungeon.treasury(request))
                    : onePositiveLong(request, parsed, amount -> action(dungeon.siege(request, amount)));
            case "!команды", "!commands" -> noArgs(request, parsed, this::commands);
            default -> null;
        };
    }

    private ChatResponse noArgs(ChatRequest request, ParsedCommand command, java.util.function.Supplier<String> response) {
        return command.args().length == 0 ? createMessage(request, response.get()) : createMessage(request, "У этой команды нет аргументов");
    }

    private ChatResponse oneTextArg(ChatRequest request, ParsedCommand command, java.util.function.Function<String, String> handler) {
        return command.args().length == 1 ? createMessage(request, handler.apply(command.args()[0])) : createMessage(request, "Нужен ровно один ID");
    }

    private ChatResponse onePositiveLong(ChatRequest request, ParsedCommand command, java.util.function.LongFunction<String> handler) {
        if (command.args().length != 1) return createMessage(request, "Нужна ровно одна положительная целая сумма");
        try {
            long value = Long.parseLong(command.args()[0]);
            return value > 0 ? createMessage(request, handler.apply(value)) : createMessage(request, "Сумма должна быть положительной");
        } catch (NumberFormatException e) {
            return createMessage(request, "Некорректная или слишком большая сумма");
        }
    }

    private String action(DungeonEconomyService.ActionResult result) { return result.getMessage(); }

    private String commands() {
        return "Команды: !данж | !босс | !стата | !артефакты | !абилка | !вклад | !казна [N] | !осада N | "
                + "!лечение | !рывок | !трофеи | !выкуп <id> | !ладдер | !герои | !инфо";
    }

    record ParsedCommand(String command, String[] args) {
        static ParsedCommand parse(String text) {
            if (text == null) return null;
            String normalized = text.trim().toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("!") || normalized.isBlank()) return null;
            String[] tokens = normalized.split("\\s+");
            String[] args = new String[Math.max(0, tokens.length - 1)];
            if (args.length > 0) System.arraycopy(tokens, 1, args, 0, args.length);
            return new ParsedCommand(tokens[0], args);
        }
    }
}
