package azhukov.chatbot.service.dunge.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public enum HeroClass {
    SAILOR("Сэйлор Пёс", "Лунное благословение", "Увеличивает атаку следующего атакующего героя"),
    DEFENDER("Защитник Будки", "Непробиваемый Щит", "Поднимает для себя дополнительный щит"),
    FAIRY("Пёсья Фея", "Исцеляющий Свет", "Немного лечит предыдущего героя"),
    NECRO("Собачий Некромант", "Призыв Скелета", "Воскрешает предыдущего павшего героя, если он мертв и его уровень ниже некроманта. Уровень воскрешенного героя уменьшается"),
    NOBLE("Благородный Догги", "Королевская Аура", "Увеличивает получаемый опыт следующего атакующего героя на 20%"),
    PRISONER("Заключённый", "Выживание", "Имеет 50% шанс выжить при смертельном исходе, сохранив 1 единицу здоровья"),
    SHAMAN("Четверолапый Шаман", "Природная Ярость", "Даёт 2 дополнительных щита следующему герою, каждый из которых поглощает 1 удар"),
    SAMURAI("Самурай", "Теневой удар", "Наносит удар из темноты, босс не отвечает"),
    WEREWOLF("Оборотень Догэн", "Обращение", "Использует случайную специальную силу другого класса"),
    ROGUE("Шелудивый Вор", "Воровство", "С 50% вероятностью ворует предмет у предыдущего героя. Если такой предмет уже есть, он превращается опыт");

    private final String label;
    private final String abilityName;
    private final String abilityDescription;

    public static final List<HeroClass> VALUES = List.of(values());

    public static HeroClass getRandomClass() {
        HeroClass[] classes = values();
        return classes[(int) (Math.random() * classes.length)];
    }
}
