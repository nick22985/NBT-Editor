package com.nick22985.nbteditor.ui.settings;

import com.nick22985.nbteditor.config.ConfigSection;
import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.module.Kernel;
import com.nick22985.nbteditor.module.ModuleInfo;
import com.nick22985.nbteditor.module.ModuleState;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.PanelScreen;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.widget.Checkbox;
import com.nick22985.nbteditor.ui.widget.MenuOption;
import com.nick22985.nbteditor.ui.widget.PopoverMenu;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.ui.widget.RowList;
import com.nick22985.nbteditor.ui.widget.TabBar;
import com.nick22985.nbteditor.ui.widget.TabSpec;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.SettingControlUtil;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SettingsScreen extends PanelScreen {
	private static final String MODULES = "modules";
	private static final int ROW_HEIGHT = 18;
	private static final int HEADER_HEIGHT = 12;
	private static final int CONTROL_WIDTH = 110;
	private static final int WIDE_CONTROL_WIDTH = 180;
	private static final int SEARCH_WIDTH = 110;

	private final Kernel kernel;
	private String tab = MODULES;
	private final Map<Setting<?>, Object> initial = new IdentityHashMap<>();
	private boolean modulesChanged;
	private String query = "";
	private RowList list;

	public SettingsScreen(Kernel kernel) {
		super(Text.translatable("nbtedit.settings.title"));
		this.kernel = kernel;
		for (ConfigSection section : kernel.config().sections()) {
			for (Setting<?> setting : section.settings()) {
				if (setting.needsRestart()) {
					this.initial.put(setting, setting.get());
				}
			}
		}
	}

	@Override
	protected Text subtitle() {
		return this.restartNeeded() ? Text.translatable("nbtedit.settings.restart") : null;
	}

	private boolean restartNeeded() {
		if (this.modulesChanged) {
			return true;
		}
		for (Map.Entry<Setting<?>, Object> entry : this.initial.entrySet()) {
			if (!Objects.equals(entry.getKey().get(), entry.getValue())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onRemoved() {
		this.kernel.config().saveIfDirty();
	}

	private void switchTab(String id) {
		this.tab = id;
		this.query = "";
		this.rebuild();
	}

	private void openOverflow(List<TabSpec> overflow, TabBar bar) {
		List<MenuOption> options = new ArrayList<>();
		for (TabSpec spec : overflow) {
			options.add(MenuOption.of(spec.label(), () -> this.switchTab(spec.id())).checked(spec.id().equals(this.tab)));
		}
		this.openOverlay(new PopoverMenu(bar.right(), bar.bottom(), false, options));
	}

	@Override
	protected void buildPanel() {
		List<TabSpec> tabs = new ArrayList<>();
		tabs.add(TabSpec.of(MODULES, Text.translatable("nbtedit.settings.modules")));
		for (ConfigSection section : this.sections()) {
			tabs.add(TabSpec.of(section.id(), Text.translatable("nbtedit." + section.id() + ".module.name")));
		}
		if (tabs.stream().noneMatch(spec -> spec.id().equals(this.tab))) {
			this.tab = MODULES;
		}
		int searchWidth = Math.min(SEARCH_WIDTH, this.bodyWidth / 3);
		TabBar bar = this.add(new TabBar(tabs, this.tab, spec -> this.switchTab(spec.id())));
		bar.bounds(this.bodyX, this.bodyY, this.bodyWidth - searchWidth - Theme.GAP, TabBar.HEIGHT);
		TextField search = this.add(new TextField(this.query).placeholder(Text.translatable("nbtedit.settings.search")).onChange(value -> {
			this.query = value;
			this.list.rows(this.rows());
		}));
		search.bounds(this.bodyX + this.bodyWidth - searchWidth, this.bodyY, searchWidth, TabBar.HEIGHT);
		int total = 0;
		for (TabSpec spec : tabs) {
			total += this.host().textWidth(spec.label()) + 14;
		}
		if (total > bar.width()) {
			bar.more(Text.translatable("nbtedit.settings.more"), overflow -> this.openOverflow(overflow, bar));
		}
		int listY = this.bodyY + TabBar.HEIGHT + Theme.GAP;
		this.list = this.add(new RowList());
		this.list.bounds(this.bodyX, listY, this.bodyWidth, this.bodyY + this.bodyHeight - listY);
		this.list.rows(this.rows());
		this.doneButton();
		if (!MODULES.equals(this.tab)) {
			this.footerButton(Text.translatable("nbtedit.settings.reset"), () -> {
				for (Setting<?> setting : this.kernel.config().section(this.tab).settings()) {
					setting.reset();
				}
				this.rebuild();
			});
		}
	}

	private List<Row> rows() {
		String needle = this.query.trim().toLowerCase(Locale.ROOT);
		if (needle.isEmpty()) {
			return MODULES.equals(this.tab) ? this.moduleRows() : this.settingRows(this.kernel.config().section(this.tab), null);
		}
		List<Row> rows = new ArrayList<>();
		for (ConfigSection section : this.sections()) {
			List<Row> matches = this.settingRows(section, needle);
			if (!matches.isEmpty()) {
				rows.add(this.header(Text.translatable("nbtedit." + section.id() + ".module.name")));
				rows.addAll(matches);
			}
		}
		if (rows.isEmpty()) {
			rows.add(this.labelled(Text.translatable("nbtedit.settings.no_matches"), ROW_HEIGHT));
		}
		return rows;
	}

	private boolean matches(ConfigSection section, Setting<?> setting, String needle) {
		String text = setting.key() + " " + this.host().translate(setting.translationKey()) + " " + this.host().translate(setting.translationKey() + ".desc") + " " + this.host().translate("nbtedit." + section.id() + ".module.name");
		return text.toLowerCase(Locale.ROOT).contains(needle);
	}

	private List<ConfigSection> sections() {
		List<ConfigSection> out = new ArrayList<>();
		for (ConfigSection section : this.kernel.config().sections()) {
			if (section.settings().stream().anyMatch(setting -> !setting.isHidden())) {
				out.add(section);
			}
		}
		return out;
	}

	private List<Row> moduleRows() {
		List<Row> rows = new ArrayList<>();
		for (ModuleInfo info : this.kernel.modules()) {
			if (info.state() == ModuleState.SKIPPED) {
				continue;
			}
			Row row = this.labelled(Text.translatable("nbtedit." + info.id() + ".module.name"), ROW_HEIGHT);
			if (info.module().canDisable()) {
				boolean enabled = this.kernel.config().moduleEnabled(info.id(), info.module().enabledByDefault());
				row.placeRight(new Checkbox(Text.EMPTY, enabled, value -> {
					this.kernel.setEnabled(info.id(), value);
					this.modulesChanged = true;
				}), 4, (ROW_HEIGHT - Checkbox.BOX) / 2, Checkbox.BOX, Checkbox.BOX);
			}
			Text state = Text.translatable("nbtedit.settings.state." + info.state().name().toLowerCase(Locale.ROOT));
			int stateColor = info.enabled() ? Theme.GREEN : info.state() == ModuleState.DISABLED ? Theme.FAINT : Theme.RED;
			row.decorate((canvas, self) -> {
				int stateX = self.x() + self.width() - 4 - Checkbox.BOX - 8 - canvas.textWidth(state);
				DrawUtil.text(canvas, state, stateX, self.y() + (ROW_HEIGHT - 8) / 2, stateColor);
			});
			rows.add(row);
		}
		return rows;
	}

	private List<Row> settingRows(ConfigSection section, String needle) {
		List<Row> rows = new ArrayList<>();
		String category = null;
		for (Setting<?> setting : section.settings()) {
			if (setting.isHidden() || needle != null && !this.matches(section, setting, needle)) {
				continue;
			}
			if (needle == null && !setting.category().equals(category)) {
				category = setting.category();
				rows.add(this.header(Text.translatable("nbtedit." + section.id() + ".config.category." + category)));
			}
			Widget control = SettingControlUtil.control(setting);
			if (control == null) {
				continue;
			}
			Text label = setting.needsRestart() ? setting.label().append(" *") : setting.label();
			Row row = this.labelled(label, ROW_HEIGHT);
			if (control instanceof Checkbox checkbox) {
				row.placeRight(checkbox, 4, (ROW_HEIGHT - Checkbox.BOX) / 2, Checkbox.BOX, Checkbox.BOX);
			} else {
				int width = SettingControlUtil.wide(setting) ? WIDE_CONTROL_WIDTH : CONTROL_WIDTH;
				row.placeRight(control, 2, 1, Math.min(width, this.bodyWidth / 2), ROW_HEIGHT - 2);
			}
			rows.add(row);
		}
		return rows;
	}

	private Row header(Text header) {
		Row row = new Row(HEADER_HEIGHT);
		row.decorate((canvas, self) -> {
			DrawUtil.text(canvas, header, self.x() + 2, self.y() + 2, Theme.GOLD);
		});
		return row;
	}

	private Row labelled(Text label, int height) {
		Row row = new Row(height);
		row.decorate((canvas, self) -> {
			DrawUtil.textClipped(canvas, label, self.x() + 4, self.y() + (height - 8) / 2, self.width() / 2 - 8, Theme.TEXT);
		});
		return row;
	}
}
