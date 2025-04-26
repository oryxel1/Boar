import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class BedrockPacksReader {
    public static void main(String[] args) {
        File clientDir = new File("C:\\Users\\PC\\Desktop\\data\\behavior_packs\\");

        final Gson gson = new Gson();

        JsonObject result = new JsonObject();
        try (final Stream<Path> stream = Files.walk(clientDir.toPath())) {
            List<File> files = stream.filter(Files::isRegularFile).map(Path::toFile).toList();

            for (final File file : files) {
                final String path = file.getAbsolutePath().replace("C:\\Users\\PC\\Desktop\\data\\behavior_packs\\", "");
                if (!path.split("\\\\")[1].equalsIgnoreCase("entities")) {
                    continue;
                }

                System.out.println(path);

                String content = new String(Files.readAllBytes(file.toPath()));
                JsonObject main = gson.fromJson(content.strip(), JsonObject.class);
                if (!main.has("minecraft:entity")) {
                    continue;
                }

                JsonObject entity = main.getAsJsonObject("minecraft:entity");
                if (!entity.has("description") || !entity.has("components")) {
                    continue;
                }

                JsonObject components = entity.getAsJsonObject("components");
                if (components == null) {
                    return;
                }

                JsonObject description = entity.getAsJsonObject("description");
                if (description == null) {
                    return;
                }

                JsonObject collisionBox = components.getAsJsonObject("minecraft:collision_box");
                if (collisionBox == null) {
                    continue;
                }

                result.add(description.get("identifier").getAsString(), components.get("minecraft:collision_box"));
            }

            Files.write(new File("C:\\Users\\PC\\Desktop\\data\\entities.json").toPath(), result.toString().getBytes());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        System.out.println(result);
    }
}
