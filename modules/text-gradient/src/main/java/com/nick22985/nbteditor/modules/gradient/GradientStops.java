package com.nick22985.nbteditor.modules.gradient;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.WidgetGroup;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.util.ColorUtil;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.ModalUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class GradientStops extends WidgetGroup {
	public static final int MIN = 2;
	public static final int MAX = 8;
	private static final int SWATCH = 20;
	private static final int TOOL = 16;
	private static final int SPACING = 2;

	private final ModuleContext ctx;
	private final List<Integer> stops = new ArrayList<>();
	private final List<Button> swatches = new ArrayList<>();
	private final Button add;
	private final Button remove;
	private final Button reverse;
	private Runnable onChange = () -> {
	};

	public GradientStops(ModuleContext ctx, int[] initial) {
		this.ctx = ctx;
		this.add = this.add(new Button(Text.literal("+"), this::addStop));
		this.add.tooltip(ctx.tr("stops.add"));
		this.remove = this.add(new Button(Text.literal("-"), this::removeStop));
		this.remove.tooltip(ctx.tr("stops.remove"));
		this.reverse = this.add(new Button(Text.literal("⇄"), this::reverseStops));
		this.reverse.tooltip(ctx.tr("stops.reverse"));
		this.load(initial);
	}

	public GradientStops onChange(Runnable onChange) {
		this.onChange = onChange;
		return this;
	}

	public int[] stops() {
		return this.stops.stream().mapToInt(Integer::intValue).toArray();
	}

	public void stops(int[] values) {
		this.load(values);
	}

	private void load(int[] values) {
		this.stops.clear();
		for (int i = 0; i < values.length && i < MAX; i++) {
			this.stops.add(values[i]);
		}
		while (this.stops.size() < MIN) {
			this.stops.add(0xFFFFFF);
		}
		this.rebuild();
	}

	private void rebuild() {
		for (Button swatch : this.swatches) {
			this.remove(swatch);
		}
		this.swatches.clear();
		for (int i = 0; i < this.stops.size(); i++) {
			int index = i;
			Button swatch = this.add(new Button(Text.EMPTY, () -> this.edit(index)));
			swatch.tooltip(Text.literal(ColorUtil.toHex(this.stops.get(i))), this.ctx.tr("stops.edit"));
			this.swatches.add(swatch);
		}
		this.add.enabled(this.stops.size() < MAX);
		this.remove.enabled(this.stops.size() > MIN);
		this.onLayout();
	}

	private void changed() {
		this.rebuild();
		this.onChange.run();
	}

	private void edit(int index) {
		UiScreen screen = this.screen();
		if (screen == null) {
			return;
		}
		ModalUtil.color(screen, this.ctx.tr("stops.title", index + 1), this.stops.get(index), rgb -> {
			this.stops.set(index, rgb);
			this.changed();
		});
	}

	private void addStop() {
		if (this.stops.size() < MAX) {
			this.stops.add(this.stops.get(this.stops.size() - 1));
			this.changed();
		}
	}

	private void removeStop() {
		if (this.stops.size() > MIN) {
			this.stops.remove(this.stops.size() - 1);
			this.changed();
		}
	}

	private void reverseStops() {
		Collections.reverse(this.stops);
		this.changed();
	}

	@Override
	protected void onLayout() {
		if (this.reverse == null) {
			return;
		}
		int left = this.x;
		for (Button swatch : this.swatches) {
			swatch.bounds(left, this.y, SWATCH, this.height);
			left += SWATCH + SPACING;
		}
		left += SPACING;
		for (Button tool : List.of(this.add, this.remove, this.reverse)) {
			tool.bounds(left, this.y, TOOL, this.height);
			left += TOOL + SPACING;
		}
	}

	@Override
	protected void renderForeground(Canvas canvas, int mouseX, int mouseY, float delta) {
		for (int i = 0; i < this.swatches.size(); i++) {
			Button swatch = this.swatches.get(i);
			DrawUtil.swatch(canvas, swatch.x() + 4, swatch.y() + 4, swatch.width() - 8, swatch.height() - 8, this.stops.get(i), false);
		}
	}
}
