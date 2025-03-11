# Chatbot Dogen

## Описание

Chatbot Dogen — это клиентское приложение для чата стримов на платформе Goodgame.ru, позволяющее обрабатывать входящие
сообщения и отвечать на них в реальном времени.

## Требования

- Java 18
- Maven 3.9.9

## Установка

1. Склонируйте репозиторий:
   ```bash
   git clone https://github.com/FerrPOINT/chatbot.git
   ```
2. Перейдите в директорию проекта:
   ```bash
   cd chatbot
   ```
3. Соберите проект с помощью Maven:
   ```bash
   mvn clean install
   ```

## Настройка

Пример конфигурационного файла `application.yml`:

```yaml
# Аутентификация для подключения к Goodgame.ru
auth:
  login: ваш_логин  # Укажите логин от Goodgame
  password: ваш_пароль  # Укажите пароль от Goodgame
checked-channels: ID_канала # ID Goodgame каналов, которые бот будет отслеживать

# Конфигурация Discord-бота
discord:
  token: ваш_токен  # Токен для подключения к Discord API

# Конфигурация Twitch-бота
twitch:
  userName: 'ваш_ник'  # Никнейм бота в Twitch
  channel: 'название_канала'  # Название Twitch-канала
  oauth-token: "ваш_oauth_token"  # OAuth-токен для аутентификации в Twitch API
```

## Использование

Запуск из командной строки:

```bash
java -jar target/chatbot.jar
```

## Вклад в проект

- Форкните репозиторий.
- Создайте новую ветку.
- Отправьте pull request.

## Лицензия

MIT License.
