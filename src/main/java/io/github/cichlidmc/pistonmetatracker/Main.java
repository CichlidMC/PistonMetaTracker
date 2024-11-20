package io.github.cichlidmc.pistonmetatracker;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.cichlidmc.pistonmetaparser.FullVersion;
import io.github.cichlidmc.pistonmetaparser.PistonMeta;
import io.github.cichlidmc.pistonmetaparser.VersionManifest;
import io.github.cichlidmc.pistonmetaparser.manifest.Version;
import io.github.cichlidmc.pistonmetaparser.version.assets.AssetIndex;
import io.github.cichlidmc.tinyjson.TinyJson;
import io.github.cichlidmc.tinyjson.value.JsonValue;
import io.github.cichlidmc.tinyjson.value.composite.JsonObject;

public class Main {
	public static void main(String[] args) throws Exception {
		Path output = Paths.get("pistonmeta");
		krill(output);
		Path versions = output.resolve("versions");
		Path indices = output.resolve("indices");
		Files.createDirectories(versions);
		Files.createDirectories(indices);

		Path manifestFile = output.resolve("manifest.json");
		JsonValue manifestJson = TinyJson.fetch(PistonMeta.URL);
		VersionManifest manifest = VersionManifest.parse(manifestJson);
		manifestJson.asObject().get("versions").asArray().stream()
				.map(JsonValue::asObject)
				.forEach(version -> {
					// these fields shift every time there's a new release and aren't really useful
					version.remove("url");
					version.remove("time");
					version.remove("sha1");
				});
		write(manifestJson, manifestFile);

		for (Version version : manifest.versions) {
			Path dest = versions.resolve(version.id + ".json");
			JsonValue fullJson = TinyJson.fetch(version.url);
			FullVersion full = FullVersion.parse(fullJson);
			JsonObject versionAssets = fullJson.asObject().get("assetIndex").asObject();
			// these also constantly change
			versionAssets.remove("sha1");
			versionAssets.remove("totalSize");
			versionAssets.remove("url");
			write(fullJson, dest);


			AssetIndex assets = full.assetIndex;
			Path assetIndex = indices.resolve(assets.id + ".json");
			if (!Files.exists(assetIndex)) {
				JsonObject assetsJson = TinyJson.fetch(assets.url).asObject();
				// filter out lang files. they constantly change too
				JsonObject objects = assetsJson.get("objects").asObject();
				// make copy to avoid CME
				Set<String> lang = objects.entrySet().stream()
						.map(Map.Entry::getKey)
						.filter(key -> key.contains("minecraft/lang/"))
						.collect(Collectors.toSet());
				lang.forEach(objects::remove);
				write(assetsJson, assetIndex);
			}
		}
	}

	private static void write(JsonValue json, Path file) throws Exception {
		Files.deleteIfExists(file);
		try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE)) {
			writer.write(json.toString());
		}
	}

	private static void krill(Path dir) throws IOException {
		if (!Files.exists(dir))
			return;
		Files.walkFileTree(dir, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
