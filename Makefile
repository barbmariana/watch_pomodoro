IMAGE := moves-build
APK := app/build/outputs/apk/debug/app-debug.apk
PKG := com.example.moves

.PHONY: help docker-image build clean install logs trigger-move trigger-water uninstall

help:
	@echo "Moves — Wear OS reminder app"
	@echo ""
	@echo "Targets:"
	@echo "  docker-image    Build the reproducible Android build container"
	@echo "  build           Build app-debug.apk inside the container"
	@echo "  clean           Gradle clean inside the container"
	@echo "  install         adb install the built APK to the connected watch"
	@echo "  uninstall       adb uninstall the app from the connected watch"
	@echo "  logs            Tail logcat for this app"
	@echo "  trigger-move    Force a Move reminder via adb broadcast"
	@echo "  trigger-water   Force a Water reminder via adb broadcast"

docker-image:
	docker build -t $(IMAGE) .

build:
	docker run --rm -v "$(CURDIR)":/workspace $(IMAGE) ./gradlew assembleDebug

clean:
	docker run --rm -v "$(CURDIR)":/workspace $(IMAGE) ./gradlew clean

install:
	adb install -r $(APK)

uninstall:
	adb uninstall $(PKG)

logs:
	adb logcat -s Moves:* AndroidRuntime:E

# Both triggers go through the same receiver; the alternation logic decides type.
# To force a specific type, set last_fired_type in DataStore beforehand or call twice.
trigger-move:
	adb shell am broadcast -a com.example.moves.REMINDER -n $(PKG)/.receiver.ReminderReceiver

trigger-water:
	adb shell am broadcast -a com.example.moves.REMINDER -n $(PKG)/.receiver.ReminderReceiver
