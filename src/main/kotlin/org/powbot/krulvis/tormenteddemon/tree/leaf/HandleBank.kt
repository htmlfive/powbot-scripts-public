package org.powbot.krulvis.tormenteddemon.tree.leaf

import org.powbot.api.Notifications
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.script.tree.Leaf
import org.powbot.krulvis.api.ATContext.emptyExcept
import org.powbot.krulvis.api.ATContext.missingHP
import org.powbot.krulvis.api.extensions.items.Food
import org.powbot.krulvis.api.extensions.Utils.waitFor
import org.powbot.krulvis.tormenteddemon.TormentedDemon
import org.powbot.mobile.script.ScriptManager

class HandleBank(script: TormentedDemon) : Leaf<TormentedDemon>(script, "Handle bank") {
	override fun execute() {
		if (script.lastTrip) {
			Notifications.showNotification("Stopping because this was the last trip")
			ScriptManager.stop()
			return
		}
		script.bankTeleport.executed = false
		val ids = (script.requiredInventory.flatMap { it.item.ids.toList() } + script.allEquipmentItems.flatMap { it.ids.toList() }).toIntArray()
		val edibleFood = foodToEat()
		if (!Inventory.emptyExcept(*ids)) {
			Bank.depositAllExcept(*ids)
		} else if (edibleFood != null) {
			edibleFood.eat()
			waitFor { foodToEat() == null }
		} else if (handleEquipment() && foodToEat() == null) {

			script.requiredInventory.forEach {
				if (!it.withdraw(true) && !it.item.inBank()) {
					script.logger.info("Stopped because no ${it.item.itemName} in bank")
					ScriptManager.stop()
				}
			}

			script.forcedBanking = script.requiredInventory.all { it.meets() }
			if (!script.forcedBanking) Bank.close()
		}

	}

	fun foodToEat() = Food.values().firstOrNull { it.inInventory() && it.healing <= missingHP() }

	fun handleEquipment(): Boolean {
		script.equipment.forEach {
			if (!it.meets()) {
				it.withdrawAndEquip(true)
			}
		}
		script.allEquipmentItems.forEach {
			if (!it.hasWith()) {
				it.withdrawExact(1, true, wait = true)
			}
		}
		script.teleportEquipments.forEach {
			if (!it.hasWith()) {
				it.withdrawExact(1, true, wait = true)
			}
		}
		return script.allEquipmentItems.all { it.hasWith() } && script.teleportEquipments.all { it.hasWith() }
	}
}