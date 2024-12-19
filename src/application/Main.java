package application;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Song {
    private final String title;
    private final String filePath;

    public Song(String title, String filePath) {
        this.title = title;
        this.filePath = filePath;
    }

    public String getTitle() {
        return title;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return title;
    }
}

public class Main extends Application {
    private final List<Song> songQueue = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private int currentSongIndex = 0;
    private Label durationLabel;
    private Canvas visualizerCanvas;
    private boolean isRepeatOn = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        String css = getClass().getResource("/resources/Style.css").toExternalForm();
        BorderPane root = new BorderPane();
        ListView<Song> songListView = new ListView<>();
        visualizerCanvas = new Canvas(400, 200);

        stage.getIcons().add(new Image("/images/logo.png"));
        
        durationLabel = new Label("0:00 / 0:00");

        Button playPauseButton = new Button("\u25B6");
        Button skipForwardButton = new Button("\u23ED");
        Button skipBackwardButton = new Button("\u23EE");
        Button addButton = new Button("\u2795");
        Button removeButton = new Button("\u2796");
        Button shuffleButton = new Button("\uD83D\uDD00");
        Button repeatButton = new Button("\uD83D\uDD04");

        HBox controls = new HBox(10, playPauseButton, skipBackwardButton, skipForwardButton, addButton, removeButton, shuffleButton, repeatButton, durationLabel);
        controls.setAlignment(Pos.CENTER);
        controls.getStyleClass().add("controls");

        HBox visualizerBox = new HBox(visualizerCanvas);
        visualizerBox.setAlignment(Pos.CENTER);

        HBox mainContent = new HBox(10, songListView, visualizerBox);
        root.setCenter(mainContent);
        root.setBottom(controls);

        playPauseButton.setOnAction(e -> togglePlayPause(playPauseButton));
        skipForwardButton.setOnAction(e -> skipForward());
        skipBackwardButton.setOnAction(e -> skipBackward());
        addButton.setOnAction(e -> addSongs(songListView, stage));
        removeButton.setOnAction(e -> removeSong(songListView));
        shuffleButton.setOnAction(e -> shuffleSongs(songListView));
        repeatButton.setOnAction(e -> toggleRepeat(repeatButton));

        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add(css);  
        stage.setTitle("Spiro");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setMaximized(false);
        stage.setFullScreenExitHint("");
        stage.setFullScreen(false);

        stage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            visualizerCanvas.setWidth(newWidth.doubleValue() - 220);
            updateVisualizer(new float[128]);
        });

        stage.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            visualizerCanvas.setHeight(newHeight.doubleValue() - 120);
            updateVisualizer(new float[128]);
        });

        GraphicsContext gc = visualizerCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, visualizerCanvas.getWidth(), visualizerCanvas.getHeight());

        stage.show();
    }

    private void togglePlayPause(Button playPauseButton) {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playPauseButton.setText("\u25B6");
        } else {
            if (mediaPlayer == null || mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED) {
                play();
            } else {
                mediaPlayer.play();
                playPauseButton.setText("\u23F8");
            }
        }
    }

    private void toggleRepeat(Button repeatButton) {
        isRepeatOn = !isRepeatOn;
        repeatButton.setText(isRepeatOn ? "\uD83D\uDD01" : "\uD83D\uDD04");
    }

    private void play() {
        if (!songQueue.isEmpty()) {
            if (mediaPlayer != null) mediaPlayer.stop();

            Song song = songQueue.get(currentSongIndex);
            Media media = new Media(song.getFilePath());
            mediaPlayer = new MediaPlayer(media);

            mediaPlayer.setOnReady(() -> mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> 
                updateDurationLabel(newValue, mediaPlayer.getMedia().getDuration())
            ));

            mediaPlayer.setOnEndOfMedia(this::playNextSong);
            mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> updateVisualizer(magnitudes));

            mediaPlayer.play();
        }
    }

    private void updateDurationLabel(javafx.util.Duration currentTime, javafx.util.Duration totalDuration) {
        durationLabel.setText(String.format("%02d:%02d / %02d:%02d",
                (int) currentTime.toMinutes(), (int) (currentTime.toSeconds() % 60),
                (int) totalDuration.toMinutes(), (int) (totalDuration.toSeconds() % 60)));
    }

    private void playNextSong() {
        if (isRepeatOn) play();
        else if (currentSongIndex < songQueue.size() - 1) currentSongIndex++;
        else currentSongIndex = 0;
        play();
    }

    private void skipForward() {
        if (!songQueue.isEmpty()) {
            if (mediaPlayer != null) mediaPlayer.stop();
            currentSongIndex = (currentSongIndex < songQueue.size() - 1) ? currentSongIndex + 1 : 0;
            play();
        }
    }

    private void skipBackward() {
        if (!songQueue.isEmpty()) {
            if (mediaPlayer != null) mediaPlayer.stop();
            currentSongIndex = (currentSongIndex > 0) ? currentSongIndex - 1 : songQueue.size() - 1;
            play();
        }
    }

    private void addSongs(ListView<Song> songListView, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP3 Files", "*.mp3"));
        List<File> files = fileChooser.showOpenMultipleDialog(stage);
        if (files != null) files.forEach(file -> {
            Song song = new Song(file.getName(), file.toURI().toString());
            songQueue.add(song);
            songListView.getItems().add(song);
        });
    }

    private void removeSong(ListView<Song> songListView) {
        Song selected = songListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            songQueue.remove(selected);
            songListView.getItems().remove(selected);
        }
    }

    private void shuffleSongs(ListView<Song> songListView) {
        Collections.shuffle(songQueue);
        songListView.getItems().setAll(songQueue);
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) mediaPlayer.stop();
    }

    private void updateVisualizer(float[] magnitudes) {
        GraphicsContext gc = visualizerCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, visualizerCanvas.getWidth(), visualizerCanvas.getHeight());

        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            gc.setFill(javafx.scene.paint.Color.BLUE);
            double barWidth = 25, gap = 5, maxBarHeight = visualizerCanvas.getHeight();
            int totalBars = (int) (visualizerCanvas.getWidth() / (barWidth + gap));

            for (int j = 0; j < totalBars && j < magnitudes.length; j++) {
                double barHeight = (magnitudes[j] + 60) / 60 * maxBarHeight;
                gc.fillRect(j * (barWidth + gap), maxBarHeight - barHeight, barWidth, barHeight);
            }
        }
    }
}
