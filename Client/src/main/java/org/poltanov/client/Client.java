package org.poltanov.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * Класс Client представляет собой клиентскую часть онлайн-игры в слова.
 * Он отвечает за взаимодействие пользователя с игрой: подключение к серверу, отправка сообщений, отображение состояния игрока и взаимодействие с GUI.
 */
public class Client extends Application {
    private String serverAddress;
    private int serverPort;
    private String playerName;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private TextArea messagesArea;
    private Button disconnectButton;
    private Button connectButton;
    private Button aboutButton;
    private VBox root;
    private VBox lobbyScreen;
    private HBox mainLayout;
    private VBox gameControls;
    private Label timeField;
    private Button sendButton;
    private TextField messageField;
    private TextArea historyArea;
    private VBox playerStatusBox;
    private List<String> playerNames = new ArrayList<>();
    private int moveNumber;


    public static void main(String[] args) {
        launch(args);
    }
    /**
     * Метод handleServerResponse обрабатывает ответ сервера на ход игрока.
     */
    private void handleServerResponse(int position, int response) {
        Platform.runLater(() -> {
            TextField letterField = (TextField) ((HBox) gameControls.getChildren().get(gameControls.getChildren().size() - 1)).getChildren().get(position);
            switch (response) {
                case -1:
                    letterField.setStyle("-fx-text-fill: red;");
                    break;
                case 0:
                    letterField.setStyle("-fx-text-fill: orange;");
                    break;
                case 1:
                    letterField.setStyle("-fx-text-fill: green;");
                    letterField.setEditable(false);
                    break;
            }
        });
    }


    /**
     * Метод start подготавливает главное окно приложения и все его компоненты.
     */
    @Override
    public void start(Stage primaryStage) {
        root = new VBox();

        Label addressLabel = new Label("Server Address:");
        TextField addressField = new TextField("localhost");

        Label portLabel = new Label("Server Port:");
        TextField portField = new TextField("1234");

        Label nameLabel = new Label("Player Name:");
        TextField nameField = new TextField();

        playerStatusBox = new VBox();
        playerStatusBox.setVisible(false);

        connectButton = new Button("Game");
        aboutButton = new Button("About");
        disconnectButton = new Button("Exit");
        disconnectButton.setDisable(true);

        messagesArea = new TextArea();
        messagesArea.setEditable(false);


        messageField = new TextField();
        messageField.setDisable(true);

        sendButton = new Button("Send");
        sendButton.setDisable(true);
        sendButton.setOnAction(e -> {
            String message = messageField.getText();
            sendMessageToServer(message);
            messageField.clear();
        });

        connectButton.setOnAction(e -> {
            serverAddress = addressField.getText();
            serverPort = Integer.parseInt(portField.getText());
            playerName = nameField.getText();
            connectButton.setDisable(true);
            disconnectButton.setDisable(false);
            new Thread(() -> {
                try {
                    connectToServer();
                    Platform.runLater(() -> {
                        sendButton.setDisable(false);
                        messageField.setDisable(false);
                    });
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        connectButton.setDisable(false);
                        disconnectButton.setDisable(true);
                    });
                }
            }).start();
        });

        disconnectButton.setOnAction(e -> {
            try {
                disconnectFromServer();
                sendButton.setDisable(true);
                messageField.setDisable(true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            connectButton.setDisable(false);
            disconnectButton.setDisable(true);
        });

        lobbyScreen = new VBox(
                addressLabel, addressField,
                portLabel, portField,
                nameLabel, nameField,
                connectButton, aboutButton, disconnectButton,
                new Label("Messages:"), messagesArea,
                new Label("Enter message:"), messageField, sendButton
        );

        gameControls = createGameControls();
        gameControls.setVisible(false);

        mainLayout = new HBox(lobbyScreen, gameControls);
        root.getChildren().add(mainLayout);

        primaryStage.setOnCloseRequest(event -> {
            try {
                disconnectFromServer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        aboutButton.setOnAction(e -> {
            Stage aboutStage = new Stage();
            VBox aboutRoot = new VBox();
            Scene aboutScene = new Scene(aboutRoot, 300, 200);
            aboutStage.setScene(aboutScene);
            aboutStage.setTitle("About");

            Label authorLabel = new Label("Author: Poltanov Svyatoslav from БПИ221");
            Label projectLabel = new Label("Online Word Game with player matchmaking system\n" +
                    "and chat in the lobby.");

            aboutRoot.getChildren().addAll(authorLabel, projectLabel);

            aboutStage.show();
        });

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Client");
        primaryStage.show();
    }
    /**
     * Метод createGameControls создает элементы управления для игрового сессии.
     */
    private VBox createGameControls() {
        VBox gameRoot = new VBox();

        playerStatusBox.setVisible(true);
        gameRoot.getChildren().add(playerStatusBox);

        Label historyLabel = new Label("Move History:");
        historyArea = new TextArea();
        historyArea.setEditable(false);

        Label timeLabel = new Label("Time Left:");
        timeField = new Label();


        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> {
            try {
                disconnectFromServer();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Platform.runLater(() -> switchToLobbyScreen());
        });


        gameRoot.getChildren().addAll(
                historyLabel, historyArea,
                timeLabel, timeField,
                new Label("Enter word:"),
                exitButton
        );



        return gameRoot;
    }
    /**
     * Метод createPlayerStatusFields создает поля для отображения статуса игроков во время сессии.
     */
    private void createPlayerStatusFields(int wordLength) {
        for (String playerName : playerNames) {
            Label playerNameLabel = new Label(playerName);
            playerStatusBox.getChildren().add(playerNameLabel);
            HBox playerStatusFields = new HBox();
            playerStatusFields.setSpacing(5);
            for (int i = 0; i < wordLength; i++) {
                TextField statusField = new TextField();
                statusField.setPrefWidth(25);
                statusField.setEditable(false);
                playerStatusFields.getChildren().add(statusField);
            }
            System.out.println(playerStatusFields);
            playerStatusBox.getChildren().add(playerStatusFields);
        }
    }
    /**
     * Метод createWordInputFields создает поля для ввода букв игроком.
     */
    private void createWordInputFields(int wordLength) {
        HBox letterFieldsBox = new HBox();
        letterFieldsBox.setSpacing(5);
        letterFieldsBox.getChildren().clear();

        for (int i = 0; i < wordLength; i++) {
            TextField letterField = new TextField();
            letterField.setPrefWidth(25);

            TextFormatter<String> formatter = new TextFormatter<>(change ->
                    change.getControlNewText().length() <= 1 ? change : null);
            letterField.setTextFormatter(formatter);

            int position = i;
            letterField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.isEmpty()) {
                    sendMessageToServer("LETTER_INPUT " + playerName + " " + position + " " + newValue.charAt(0));
                }
            });

            letterFieldsBox.getChildren().add(letterField);
        }

        gameControls.getChildren().add(letterFieldsBox);
    }
    /**
     * Метод switchToLobbyScreen переключает интерфейс на экран лобби.
     */
    private void switchToLobbyScreen() {
        gameControls.getChildren().removeIf(node -> node instanceof HBox);
        gameControls.setVisible(false);
        lobbyScreen.setVisible(true);
        playerNames.clear();
        playerStatusBox.getChildren().clear();
    }
    /**
     * Метод switchToGameControls переключает интерфейс на экран сессии.
     */
    private void switchToGameControls() {
        lobbyScreen.setVisible(false);
        gameControls.setVisible(true);
        moveNumber = 1;
    }
    /**
     * Метод updateWordStatus обновляет отображение статуса разгадывания слова для указанного игрока.
     */
    private void updateWordStatus(int[] wordStatus, String playerName) {
        if(playerStatusBox.getChildren().size() < playerNames.size() * 2) {
            return;
        }
        int playerIndex = playerNames.indexOf(playerName);
        System.out.println(playerStatusBox.getChildren());
        for (int i = 0; i < wordStatus.length; i++) {
            Node node = playerStatusBox.getChildren().get(playerIndex * 2 + 1);
            if (node instanceof HBox) {
                TextField letterField = (TextField) ((HBox) node).getChildren().get(i);
                int status = wordStatus[i];
                System.out.println(status);
                if (status == 1) {
                    letterField.setStyle("-fx-background-color: green;");
                } else if (status == 0) {
                    letterField.setStyle("-fx-background-color: yellow;");
                } else if (status == -1) {
                    letterField.setStyle("-fx-background-color: red;");
                } else {
                    letterField.setStyle("-fx-background-color: white;");
                }
            }
        }
    }
    /**
     * Метод connectToServer подключает клиента к серверу и обрабатывает входящие сигналы.
     * SERVER_STOPPED - означает остановку сервера
     * PLAYER_INFO - информация о имени одного из игроков
     * WORD_STATUS - статус разгадывания слова другого игрока
     * MOVE - информация о сделанном ходе
     * INVALID_LETTER - возвращается, если игрок вводит недопустимый символ
     * TIME_LEFT - оставшееся время до конца сеанса
     * START_GAME - уведомление о начале игры
     * WORD_LENGTH - длина слова
     * RESET_CLIENT - просит клента вернуться в исходное состояние
     */
    public void connectToServer() throws IOException {
        try {
            socket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(playerName);
            updateMessagesArea("Connected to server at " + serverAddress + ":" + serverPort);


            new Thread(() -> {
                String serverMessage;
                try {
                    while ((serverMessage = in.readLine()) != null) {
                        if (serverMessage.equals("SERVER_STOPPED")) {
                            disconnectFromServer();
                        } else if (serverMessage.startsWith("PLAYER_INFO")) {
                            String playerName = serverMessage.split(" ")[1];
                            playerNames.add(playerName);
                        } else if (serverMessage.startsWith("WORD_STATUS")) {
                            String[] parts = serverMessage.split(" ");
                            int[] wordStatus = Arrays.stream(parts).skip(2).mapToInt(Integer::parseInt).toArray();
                            System.out.println(Arrays.toString(wordStatus));
                            System.out.println(Arrays.toString(parts));
                            if(!parts[1].equals(playerName)) {
                                updateWordStatus(wordStatus, parts[1]);
                            }
                        } else if (serverMessage.startsWith("MOVE")) {
                            String[] parts = serverMessage.split(" ");
                            String playerName = parts[1];
                            int position = Integer.parseInt(parts[2]);
                            char letter = parts[3].charAt(0);
                            int response = Integer.parseInt(parts[4]);

                            if (playerName.equals(this.playerName)) {
                                handleServerResponse(position, response);
                                Platform.runLater(() -> {
                                    historyArea.appendText("Move:" + moveNumber + ", Letter: " + letter + ", Response: " + response + "\n");
                                    moveNumber++;
                                });
                            }
                        } else if (serverMessage.startsWith("INVALID_LETTER")) {
                            Platform.runLater(() -> historyArea.appendText("Invalid letter. Please enter a lowercase Cyrillic letter.\n"));

                        } else if (serverMessage.startsWith("TIME_LEFT")) {
                            String timeLeft = serverMessage.split(" ")[1];
                            Platform.runLater(() -> timeField.setText(timeLeft));
                        } else if (serverMessage.equals("START_GAME")) {
                            Platform.runLater(this::switchToGameControls);
                        } else if (serverMessage.startsWith("WORD_LENGTH")) {
                            int wordLength = Integer.parseInt(serverMessage.split(" ")[1]);
                            Platform.runLater(() -> {
                                createWordInputFields(wordLength);
                                createPlayerStatusFields(wordLength);
                            });
                        } else if (serverMessage.equals("RESET_CLIENT")) {
                            Platform.runLater(this::resetClientInterface);
                        } else {
                            updateMessagesArea(serverMessage);
                        }
                    }
                } catch (IOException e) {
                    updateMessagesArea("Connection lost: " + e.getMessage());
                    Platform.runLater(this::resetClientInterface);
                } finally {
                    Platform.runLater(() -> {
                        connectButton.setDisable(false);
                        disconnectButton.setDisable(true);
                    });
                }
            }).start();
        } catch (IOException e) {
            updateMessagesArea("Unable to connect to server: " + e.getMessage());
            throw e;
        }
    }


    /**
     * Метод resetClientInterface сбрасывает интерфейс клиента в исходное состояние.
     */
    public void resetClientInterface() {
        switchToLobbyScreen();
        historyArea.clear();
        connectButton.setDisable(false);
        disconnectButton.setDisable(true);
    }

    /**
     * Метод disconnectFromServer отключает клиента от сервера.
     */
    public void disconnectFromServer() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        updateMessagesArea("Disconnected from server.");
        Platform.runLater(() -> {
            sendButton.setDisable(true);
            historyArea.clear();
            messageField.setDisable(true);
            connectButton.setDisable(false);
            disconnectButton.setDisable(true);
            switchToLobbyScreen();
        });
    }


    /**
     * Метод sendMessageToServer отправляет сообщение на сервер.
     */
    private void sendMessageToServer(String message) {
        out.println(message);
    }
    /**
     * Метод updateMessagesArea добавляет сообщение в область сообщений в лобби.
     */
    private void updateMessagesArea(String message) {
        Platform.runLater(() -> messagesArea.appendText(message + "\n"));
    }
}
