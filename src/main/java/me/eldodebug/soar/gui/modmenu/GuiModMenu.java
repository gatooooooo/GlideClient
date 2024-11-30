package me.eldodebug.soar.gui.modmenu;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.lwjgl.input.Keyboard;

import me.eldodebug.soar.Soar;
import me.eldodebug.soar.gui.GuiEditHUD;
import me.eldodebug.soar.gui.modmenu.category.Category;
import me.eldodebug.soar.gui.modmenu.category.impl.CosmeticsCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.HomeCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.ModuleCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.MusicCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.ProfileCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.ScreenshotCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.SettingCategory;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.ColorManager;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.event.impl.EventRenderNotification;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.Icon;
import me.eldodebug.soar.ui.comp.impl.field.CompSearchBox;
import me.eldodebug.soar.utils.MathUtils;
import me.eldodebug.soar.utils.animation.normal.Animation;
import me.eldodebug.soar.utils.animation.normal.Direction;
import me.eldodebug.soar.utils.animation.normal.easing.EaseBackIn;
import me.eldodebug.soar.utils.animation.simple.SimpleAnimation;
import me.eldodebug.soar.utils.buffer.ScreenAnimation;
import me.eldodebug.soar.utils.mouse.MouseUtils;
import me.eldodebug.soar.utils.mouse.Scroll;
import me.eldodebug.soar.utils.render.BlurUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

public class GuiModMenu extends GuiScreen {

	private Animation introAnimation;
	private int x, y, width, height;
	
	private ArrayList<Category> categories = new ArrayList<Category>();
	private Category currentCategory;
	
	private SimpleAnimation moveAnimation = new SimpleAnimation();
	
	private ScreenAnimation screenAnimation = new ScreenAnimation();
	
	private Scroll scroll = new Scroll();
	private Scroll categoryScroll = new Scroll();
	
	private boolean toEditHUD, canClose;
	
	private CompSearchBox searchBox = new CompSearchBox();
	
	public GuiModMenu() {
		
		categories.add(new HomeCategory(this));
		categories.add(new ModuleCategory(this));
		categories.add(new CosmeticsCategory(this));
		categories.add(new MusicCategory(this));
		categories.add(new ProfileCategory(this));
		categories.add(new ScreenshotCategory(this));
		categories.add(new SettingCategory(this));
		
		currentCategory = getCategoryByClass(HomeCategory.class);
	}
	
	@Override
	public void initGui() {
		
		ScaledResolution sr = new ScaledResolution(mc);
		
		int addX = 225;
		int addY = 140;
		
		x = (sr.getScaledWidth() / 2) - addX;
		y = (sr.getScaledHeight() / 2) - addY;
		width = addX * 2;
		height = addY * 2;
		
		introAnimation = new EaseBackIn(320, 1.0F, 2.0F);
		introAnimation.setDirection(Direction.FORWARDS);
		
		for(Category c : categories) {
			c.initGui();
		}
		
		scroll.resetAll();
		categoryScroll.resetAll();
		toEditHUD = false;
		canClose = true;
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		Soar instance = Soar.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		BlurUtils.drawBlurScreen((float) (Math.min(introAnimation.getValue(), 1) * 20) + 1F);
		screenAnimation.wrap(() -> {
			nvg.drawShadow(x, y, width, height, 12);
		}, 2 - introAnimation.getValueFloat(), Math.min(introAnimation.getValueFloat(), 1));
		
		screenAnimation.wrap(() -> drawNanoVG(mouseX, mouseY, partialTicks), this.getX(), this.getY(), this.getWidth(), this.getHeight(), 2 - introAnimation.getValueFloat(), Math.min(introAnimation.getValueFloat(), 1), true);
		
		new EventRenderNotification().call();
		
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
	
	private void drawNanoVG(int mouseX, int mouseY, float partialTicks) {
		
		Soar instance = Soar.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ColorManager colorManager = instance.getColorManager();
		ColorPalette palette = colorManager.getPalette();
		AccentColor currentColor = colorManager.getCurrentColor();
		File headFile = new File(instance.getFileManager().getCacheDir(), "head/" + instance.getAccountManager().getCurrentAccount().getName() + ".png");
		
		if(introAnimation.isDone(Direction.BACKWARDS)) {
			mc.displayGuiScreen(toEditHUD ? new GuiEditHUD(true) : null);
		}
		
		nvg.drawRoundedRect(x, y, width, height, 12, palette.getBackgroundColor(ColorType.NORMAL));
		nvg.drawRoundedRectVarying(x, y, 32, height, 12, 0, 12, 0, palette.getBackgroundColor(ColorType.DARK));
		nvg.drawRect(x + 32, y + 30, width - 32, 1, palette.getBackgroundColor(ColorType.DARK));
		
		nvg.drawGradientRoundedRect(x + 5, y + 7, 22, 22, 11, currentColor.getColor1(), currentColor.getColor2());
		nvg.drawText(Icon.SOAR, x + 8, y + 10, Color.WHITE, 16, Fonts.ICON);
		
		nvg.save();
		nvg.translate(currentCategory.getTextAnimation().getValue() * 15, 0);
		nvg.drawText(currentCategory.getName(), x + 32, y + 10, palette.getFontColor(ColorType.DARK, (int) (currentCategory.getTextAnimation().getValue() * 255)), 15, Fonts.MEDIUM);
		nvg.restore();
		
		int offsetY = 0;
		
		moveAnimation.setAnimation(categories.indexOf(currentCategory) * 30, 18);
		
		nvg.save();
		nvg.scissor(x, y + 30, 32, height - 100);
		nvg.translate(0, categoryScroll.getValue());
		
		nvg.drawGradientRoundedRect(x + 5.5F, y + 38.5F + moveAnimation.getValue(), 21, 21, 4, currentColor.getColor1(), currentColor.getColor2());
		
		for(Category c : categories) {
			
			Color textColor = c.getTextColorAnimation().getColor(MathUtils.isInRange(moveAnimation.getValue(), offsetY - 8, offsetY + 8) ? Color.WHITE : palette.getFontColor(ColorType.NORMAL), 18);
			
			c.getTextAnimation().setAnimation(c.equals(currentCategory) ? 1.0F : 0.0F, 14);
			
			nvg.drawText(c.getIcon(), x + 9F, y + 42 + offsetY, textColor, 14, Fonts.ICON);
			
			offsetY+=30;
		}
		
		nvg.restore();
		
		nvg.drawRect(x, y + height - 70, 32, 1, palette.getBackgroundColor(ColorType.NORMAL));
		
		if(!headFile.exists()) {
			nvg.drawPlayerHead(new ResourceLocation("textures/entity/steve.png"), x + 5, y + height - 30, 22, 22, 11);
		}else {
			nvg.drawRoundedImage(headFile, x + 5, y + height - 30, 22, 22, 11);
		}
		
		nvg.drawGradientRoundedRect(x + 5.5F, y + height - 60, 21, 21, 4, currentColor.getColor1(), currentColor.getColor2());
		nvg.drawText(Icon.LAYOUT, x + 9, y + height - 56.5F, Color.WHITE, 14, Fonts.ICON);
		
		for(Category c : categories) {
			
			c.getCategoryAnimation().setAnimation(c.equals(currentCategory) ? 1.0F : 0.0F, 16);
			
			if(c.equals(currentCategory)) {
				
				nvg.save();
				
				if(!c.isInitialized()) {
					c.setInitialized(true);
					c.initCategory();
					searchBox.setText("");
					c.setCanClose(true);
				}
				
				if(c.isShowSearchBox()) {
					searchBox.setPosition(x + width - 175, y + 6.5F, 160, 18);
					searchBox.draw(mouseX, mouseY, partialTicks);
				}
				
				nvg.scissor(x + 32, y + 31, width - 32, height - 31);
				nvg.translate(0, 50 - (c.getCategoryAnimation().getValue() * 50));
				
				c.drawScreen(mouseX, mouseY, partialTicks);
				
				nvg.restore();
				
			}else if(c.isInitialized()) {
				c.setInitialized(false);
			}
		}
		
		if(MouseUtils.isInside(mouseX, mouseY, x + 32, y + 31, width - 32, height - 31)) {
			scroll.onScroll();
		}
		
		scroll.onAnimation();
		
		if(MouseUtils.isInside(mouseX, mouseY, x, y, 32, height)) {
			categoryScroll.onScroll();
		}
		
		categoryScroll.onAnimation();
		
		categoryScroll.setMaxScroll((categories.size() - 5.85F) * 30);
		
		if(currentCategory.isShowSearchBox() && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_F)) {
			currentCategory.getSearchBox().setFocused(true);
		}
	}
	
	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		
		int offsetY = 0;
		
		offsetY = (int) (offsetY + categoryScroll.getValue());
		
		for(Category c : categories) {
			
			if(MouseUtils.isInside(mouseX, mouseY, x, y + 30, 32, height - 100) && MouseUtils.isInside(mouseX, mouseY, x + 5.5F, y + 38.5F + offsetY, 21, 21) && mouseButton == 0) {
				currentCategory = c;
			}
			
			offsetY+=30;
		}
		
		if(MouseUtils.isInside(mouseX, mouseY, x + 5.5F, y + height - 60, 21, 21) && mouseButton == 0) {
			toEditHUD = true;
			introAnimation.setDirection(Direction.BACKWARDS);
		}
		
		currentCategory.mouseClicked(mouseX, mouseY, mouseButton);
		searchBox.mouseClicked(mouseX, mouseY, mouseButton);
		
		try {
			super.mouseClicked(mouseX, mouseY, mouseButton);
		} catch (IOException e) {}
	}
	
	@Override
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
		currentCategory.mouseReleased(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public void keyTyped(char typedChar, int keyCode) {
		
		currentCategory.keyTyped(typedChar, keyCode);
		searchBox.keyTyped(typedChar, keyCode);
		
		if(currentCategory.isShowSearchBox() && canClose) {
			
			if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
				
				if(!searchBox.getText().isEmpty()) {
					searchBox.setText("");
					searchBox.setFocused(false);
					return;
				}
				
				if(searchBox.isFocused()) {
					searchBox.setFocused(false);
					return;
				}
			}
		}
		
		if(keyCode == Keyboard.KEY_ESCAPE && canClose) {
			introAnimation.setDirection(Direction.BACKWARDS);
		}
	}
	
	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
	
	@Override
	public void onGuiClosed() {
		Soar.getInstance().getProfileManager().save();
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public ArrayList<Category> getCategories() {
		return categories;
	}
	
	public Category getCategoryByClass(Class<?> clazz) {
		
		for(Category c : categories) {
			if(c.getClass().equals(clazz)) {
				return c;
			}
		}
		
		return null;
	}

	public Scroll getScroll() {
		return scroll;
	}

	public CompSearchBox getSearchBox() {
		return searchBox;
	}

	public boolean isCanClose() {
		return canClose;
	}

	public void setCanClose(boolean canClose) {
		this.canClose = canClose;
	}
}