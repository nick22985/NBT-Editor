package com.nick22985.nbteditor.smoke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class SmokeContext {
	private final AtomicLong ticks = new AtomicLong();
	private final List<String> lines = new ArrayList<>();
	private int failures;

	public void tick() {
		this.ticks.incrementAndGet();
	}

	public Minecraft client() {
		return Minecraft.getInstance();
	}

	public void waitTicks(int count) {
		long target = this.ticks.get() + count;
		long deadline = System.currentTimeMillis() + 120000L + count * 1000L;
		while (this.ticks.get() < target) {
			if (System.currentTimeMillis() > deadline) {
				throw new AssertionError("Timed out waiting " + count + " ticks");
			}
			this.sleep(10);
		}
	}

	public void waitFor(String what, int seconds, Function<Minecraft, Boolean> check) {
		long deadline = System.currentTimeMillis() + seconds * 1000L;
		while (!Boolean.TRUE.equals(this.compute(check))) {
			if (System.currentTimeMillis() > deadline) {
				throw new AssertionError("Timed out waiting for " + what);
			}
			this.sleep(100);
		}
	}

	public <T> T compute(Function<Minecraft, T> task) {
		Minecraft client = this.client();
		try {
			return client.submit(() -> task.apply(client)).get(120, TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			if (cause instanceof Error) {
				throw (Error) cause;
			}
			throw new AssertionError(cause);
		} catch (InterruptedException | TimeoutException e) {
			throw new AssertionError(e);
		}
	}

	public void run(Consumer<Minecraft> task) {
		this.compute(client -> {
			task.accept(client);
			return null;
		});
	}

	public void screenshot(String name) {
		this.waitTicks(2);
		this.run(client -> Screenshot.grab(client.gameDirectory, "smoke-" + name + ".png", client.getMainRenderTarget(), message -> {}));
	}

	public void step(String name, Runnable body) {
		try {
			body.run();
			this.lines.add("PASS " + name);
		} catch (Throwable t) {
			this.failures++;
			this.lines.add("FAIL " + name + ": " + t);
			System.out.println("[Smoke] " + name + " failed");
			t.printStackTrace(System.out);
		}
	}

	public void note(String line) {
		this.lines.add("NOTE " + line);
	}

	public void finish() {
		this.lines.add(this.failures == 0 ? "RESULT ok" : "RESULT " + this.failures + " failed");
		Path file = this.client().gameDirectory.toPath().resolve("smoke-result.txt");
		try {
			Files.write(file, this.lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace(System.out);
		}
		for (String line : this.lines) {
			System.out.println("[Smoke] " + line);
		}
		this.run(Minecraft::stop);
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
	}
}
