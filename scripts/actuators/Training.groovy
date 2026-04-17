import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.sokybot.behaviors.training.api.TrainingSettings
import org.sokybot.engine.api.behavior.BehaviorCycleSpec
import org.sokybot.engine.api.behavior.BehaviorStatus
import org.sokybot.engine.api.behavior.IBehavior
import org.sokybot.engine.api.behavior.IBehaviorCycleAssembler
import org.sokybot.engine.api.workflow.IWorkflowContext

class Training extends BaseActuator {

    Training() { super("training") }

    @Override
    void setup() {
        registerSettings("training", TrainingSettings, { new TrainingSettings() })
        def settingsProvider = settingsProvider("training", TrainingSettings)
        def assembler = context.getService(IBehaviorCycleAssembler)

        if (settingsProvider == null || assembler == null) {
            log.warn("Training: settings provider or behavior assembler unavailable; cycle will not be registered")
            return
        }

        def spec = BehaviorCycleSpec.builder(TrainingSettings)
                .cycleId("training-cycle")
                .priority(300)
                .settingsSupplier({ settingsProvider?.get() })
                .entryGuard({ ctx ->
                    def settings = settingsProvider?.get()
                    return settings != null && isLoggedIn(ctx) && settings.isAutoAttack()
                })
                .interruptible(true)
                .interruptionAction({ ctx -> log.warn("Interrupting training due to low HP/MP") })
                .build()

        context.getWorkflowRegistry().registerCycle(assembler.assemble(spec))
        log.info("Training cycle registered via behavior assembler")
    }

    private boolean isLoggedIn(def ctx) {
        try {
            def trainer = ctx.getGameModel().getTrainer()
            return trainer != null && trainer.getUniqueId() > 0
        } catch (Exception e) {
            return false
        }
    }
}

@Deprecated
class PotionBehavior {
    private static final Logger log = LoggerFactory.getLogger(PotionBehavior)

    boolean needsPotion(IWorkflowContext context, TrainingSettings settings) {
        def behavior = behaviorLookup(context, "potion")
        return behavior != null && behavior.applies(context, settings)
    }

    void execute(IWorkflowContext context, TrainingSettings settings) {
        def behavior = behaviorLookup(context, "potion")
        if (behavior == null) return
        def status = behavior.execute(context, settings)
        if (status == BehaviorStatus.INTERRUPT) {
            log.debug("Deprecated PotionBehavior shim received INTERRUPT status")
        }
    }
}

@Deprecated
class CombatBehavior {
    private static final Logger log = LoggerFactory.getLogger(CombatBehavior)

    boolean shouldAttack(IWorkflowContext context, TrainingSettings settings) {
        def behavior = behaviorLookup(context, "combat")
        return behavior != null && behavior.applies(context, settings)
    }

    void execute(IWorkflowContext context, TrainingSettings settings) {
        def behavior = behaviorLookup(context, "combat")
        if (behavior == null) return
        def status = behavior.execute(context, settings)
        if (status == BehaviorStatus.INTERRUPT) {
            log.debug("Deprecated CombatBehavior shim received INTERRUPT status")
        }
    }
}

@Deprecated
class TownBehavior {
    private static final Logger log = LoggerFactory.getLogger(TownBehavior)

    boolean shouldReturnToTown(IWorkflowContext context) {
        def behavior = behaviorLookup(context, "town")
        return behavior != null && behavior.applies(context, null)
    }

    void execute(IWorkflowContext context) {
        def behavior = behaviorLookup(context, "town")
        if (behavior == null) return
        behavior.execute(context, null)
    }
}

@SuppressWarnings("unchecked")
private static IBehavior<TrainingSettings> behaviorLookup(IWorkflowContext context, String id) {
    try {
        def bundleContext = context.getService(org.osgi.framework.BundleContext)
        if (bundleContext == null) return null
        def refs = bundleContext.getServiceReferences(IBehavior.class.name, "(component.name=*${id}*)")
        if (refs == null || refs.isEmpty()) return null
        def service = bundleContext.getService(refs.iterator().next())
        return (IBehavior<TrainingSettings>) service
    } catch (Exception ignored) {
        return null
    }
}

new Training()
