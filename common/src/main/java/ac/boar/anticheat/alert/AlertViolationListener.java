package ac.boar.anticheat.alert;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.violation.Violation;
import ac.boar.anticheat.violation.ViolationListener;

public final class AlertViolationListener implements ViolationListener {
    @Override
    public void onViolation(Violation violation) {
        final Check check = violation.check();

        String message = Boar.getConfig().messages().alertFormat()
                .replace("{player}", violation.player().getSession().name())
                .replace("{check}", check.name())
                .replace("{type}", check.type())
                .replace("{experimental}", check.experimental() ? " §a(Experimental)" : "")
                .replace("{level}", String.valueOf(violation.vl()))
                .replace("{reason}", violation.verbose())
                .replace("&", "§");

        Boar.getInstance().getAlertManager().alert(message);
    }
}
