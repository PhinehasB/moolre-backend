package com.project.klare_server.auth.notification;

final class EmailTemplates {

    private EmailTemplates() {
    }

    static String verification(String firstName, String companyName, String verificationLink) {
        String body = """
                <h1 style="margin:0 0 16px;font-size:24px;line-height:1.3;color:#0f2a21;font-weight:700;">Verify your company</h1>
                <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:#43564f;">Hi %s,</p>
                <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#43564f;">
                  Welcome to Klare. You're one click away from activating <strong style="color:#0f2a21;">%s</strong>
                  and its Moolre business wallet. Verify your email to finish setting up and sign in.
                </p>
                %s
                <p style="margin:24px 0 0;font-size:13px;line-height:1.6;color:#8a978f;">
                  This link expires in 48 hours. If you didn't create a Klare account, you can safely ignore this email.
                </p>
                """.formatted(escape(firstName), escape(companyName), button("Verify my company", verificationLink));
        return layout("Verify your Klare company", "Verify your email to activate your Klare wallet", body, verificationLink, "Verify my company");
    }

    static String passwordReset(String firstName, String resetLink) {
        String body = """
                <h1 style="margin:0 0 16px;font-size:24px;line-height:1.3;color:#0f2a21;font-weight:700;">Reset your password</h1>
                <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:#43564f;">Hi %s,</p>
                <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#43564f;">
                  We received a request to reset your Klare password. Click the button below to choose a new one.
                </p>
                %s
                <p style="margin:24px 0 0;font-size:13px;line-height:1.6;color:#8a978f;">
                  This link expires in 30 minutes. If you didn't request this, you can safely ignore this email and your password stays the same.
                </p>
                """.formatted(escape(firstName), button("Reset password", resetLink));
        return layout("Reset your Klare password", "Reset your Klare password", body, resetLink, "Reset password");
    }

    static String employeeInvitation(String firstName, String companyName) {
        String body = """
                <h1 style="margin:0 0 16px;font-size:24px;line-height:1.3;color:#0f2a21;font-weight:700;">You've been added to payroll</h1>
                <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:#43564f;">Hi %s,</p>
                <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#43564f;">
                  <strong style="color:#0f2a21;">%s</strong> has added you to their team on Klare. Download the Klare app to
                  link your wallet, set up your bills, and get paid automatically every payday.
                </p>
                <p style="margin:0 0 0;font-size:15px;line-height:1.6;color:#43564f;">Your salary, already sorted.</p>
                """.formatted(escape(firstName), escape(companyName));
        return layout("You've been added to payroll on Klare", "You've been added to payroll on Klare", body, null, null);
    }

    static String employeeCredentials(String firstName, String companyName, String username, String temporaryPassword) {
        String credentials = """
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="margin:8px 0 4px;border:1px solid #e3e9e5;border-radius:12px;">
                  <tr>
                    <td style="padding:16px 20px;border-bottom:1px solid #eef1ee;">
                      <p style="margin:0 0 4px;font-size:12px;letter-spacing:0.4px;text-transform:uppercase;color:#8a978f;">Username</p>
                      <p style="margin:0;font-size:18px;font-weight:700;color:#0f2a21;">%s</p>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:16px 20px;">
                      <p style="margin:0 0 4px;font-size:12px;letter-spacing:0.4px;text-transform:uppercase;color:#8a978f;">Temporary password</p>
                      <p style="margin:0;font-size:18px;font-weight:700;letter-spacing:1px;color:#0f2a21;">%s</p>
                    </td>
                  </tr>
                </table>
                """.formatted(escape(username), escape(temporaryPassword));
        String body = """
                <h1 style="margin:0 0 16px;font-size:24px;line-height:1.3;color:#0f2a21;font-weight:700;">Your sign-in details</h1>
                <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:#43564f;">Hi %s,</p>
                <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#43564f;">
                  <strong style="color:#0f2a21;">%s</strong> added you to their team on Klare. Open the Klare app and sign in
                  with the details below. You'll be asked to set your own password to activate your wallet.
                </p>
                %s
                <p style="margin:20px 0 0;font-size:13px;line-height:1.6;color:#8a978f;">
                  For your security, change this temporary password as soon as you sign in. Never share it with anyone.
                </p>
                """.formatted(escape(firstName), escape(companyName), credentials);
        return layout("Your Klare sign-in details", "Your Klare username and temporary password", body, null, null);
    }

    static String payrollEstimate(String firstName, String companyName, String payDate, String total, int employees) {
        String body = """
                <h1 style="margin:0 0 16px;font-size:24px;line-height:1.3;color:#0f2a21;font-weight:700;">Payroll is coming up</h1>
                <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:#43564f;">Hi %s,</p>
                <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#43564f;">
                  Klare will automatically run payroll for <strong style="color:#0f2a21;">%s</strong> on <strong>%s</strong>.
                  Here's the estimate so you can make sure your wallet is funded:
                </p>
                <p style="margin:0 0 6px;font-size:15px;line-height:1.8;color:#43564f;">Employees: <strong style="color:#0f2a21;">%d</strong></p>
                <p style="margin:0 0 0;font-size:15px;line-height:1.8;color:#43564f;">Total to pay: <strong style="color:#0f2a21;">GHS %s</strong></p>
                """.formatted(escape(firstName), escape(companyName), escape(payDate), employees, escape(total));
        return layout("Your upcoming Klare payroll", "Your upcoming Klare payroll estimate", body, null, null);
    }

    static String automaticPayrollComplete(String firstName, String companyName, String amount, int paid, int failed) {
        String failedLine = failed > 0
                ? "<p style=\"margin:0;font-size:15px;line-height:1.8;color:#b4453a;\">Failed: <strong>" + failed + "</strong> (we'll retry and update you)</p>"
                : "";
        String body = """
                <h1 style="margin:0 0 16px;font-size:24px;line-height:1.3;color:#0f2a21;font-weight:700;">Payroll has been paid</h1>
                <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:#43564f;">Hi %s,</p>
                <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#43564f;">
                  Klare just ran payroll automatically for <strong style="color:#0f2a21;">%s</strong>. Your team has been paid.
                </p>
                <p style="margin:0 0 6px;font-size:15px;line-height:1.8;color:#43564f;">Paid: <strong style="color:#0f2a21;">%d employees</strong></p>
                <p style="margin:0 0 6px;font-size:15px;line-height:1.8;color:#43564f;">Total disbursed: <strong style="color:#0f2a21;">GHS %s</strong></p>
                %s
                """.formatted(escape(firstName), escape(companyName), paid, escape(amount), failedLine);
        return layout("Klare payroll complete", "Klare ran your payroll", body, null, null);
    }

    static String topUpReminder(String firstName, String companyName, String shortfall, String payDate) {
        String body = """
                <h1 style="margin:0 0 16px;font-size:24px;line-height:1.3;color:#0f2a21;font-weight:700;">Top up to run payroll</h1>
                <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:#43564f;">Hi %s,</p>
                <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#43564f;">
                  Payroll for <strong style="color:#0f2a21;">%s</strong> was due on <strong>%s</strong>, but your wallet
                  doesn't have enough to cover it. We didn't pay anyone so nothing is half-done.
                </p>
                <p style="margin:0 0 24px;font-size:15px;line-height:1.8;color:#43564f;">
                  Please top up at least <strong style="color:#b4453a;">GHS %s</strong> and Klare will run it on the next cycle —
                  or run it manually from your dashboard once funded.
                </p>
                """.formatted(escape(firstName), escape(companyName), escape(payDate), escape(shortfall));
        return layout("Top up to run your Klare payroll", "Top up to run your Klare payroll", body, null, null);
    }

    static String payrollCode(String firstName, String code) {
        String body = """
                <h1 style="margin:0 0 16px;font-size:24px;line-height:1.3;color:#0f2a21;font-weight:700;">Confirm your payroll</h1>
                <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:#43564f;">Hi %s,</p>
                <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#43564f;">
                  Use this code to confirm and run your payroll. It expires in 10 minutes.
                </p>
                <p style="margin:0 0 8px;font-size:34px;line-height:1.2;letter-spacing:8px;color:#15604a;font-weight:700;">%s</p>
                <p style="margin:16px 0 0;font-size:13px;line-height:1.6;color:#8a978f;">
                  If you didn't start a payroll, ignore this email and do not share the code with anyone.
                </p>
                """.formatted(escape(firstName), escape(code));
        return layout("Your Klare payroll code", "Your Klare payroll confirmation code", body, null, null);
    }

    static String paydayReminder(String firstName, String companyName, String payDate) {
        String body = """
                <h1 style="margin:0 0 16px;font-size:24px;line-height:1.3;color:#0f2a21;font-weight:700;">Payday is almost here</h1>
                <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:#43564f;">Hi %s,</p>
                <p style="margin:0 0 0;font-size:15px;line-height:1.6;color:#43564f;">
                  Your salary from <strong style="color:#0f2a21;">%s</strong> is scheduled to arrive on <strong>%s</strong>
                  through Klare. Your money, already sorted.
                </p>
                """.formatted(escape(firstName), escape(companyName), escape(payDate));
        return layout("Payday is almost here", "Your Klare payday is almost here", body, null, null);
    }

    private static String button(String label, String href) {
        return """
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin:8px 0;">
                  <tr>
                    <td align="center" bgcolor="#15604a" style="border-radius:10px;">
                      <a href="%s" target="_blank"
                         style="display:inline-block;padding:14px 28px;font-size:15px;font-weight:600;color:#ffffff;text-decoration:none;border-radius:10px;background:#15604a;">
                        %s
                      </a>
                    </td>
                  </tr>
                </table>
                """.formatted(href, label);
    }

    private static String layout(String title, String preheader, String body, String fallbackLink, String fallbackLabel) {
        String fallback = fallbackLink == null ? "" : """
                <p style="margin:20px 0 0;font-size:13px;line-height:1.6;color:#8a978f;">
                  Or paste this link into your browser:<br>
                  <a href="%s" style="color:#15604a;word-break:break-all;">%s</a>
                </p>
                """.formatted(fallbackLink, fallbackLink);

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:#eef1ee;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <span style="display:none;max-height:0;overflow:hidden;opacity:0;">%s</span>
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#eef1ee;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;width:100%%;">
                          <tr>
                            <td style="padding:8px 4px 20px;">
                              <table role="presentation" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td style="width:34px;height:34px;background:#15604a;border-radius:9px;text-align:center;vertical-align:middle;color:#ffffff;font-size:18px;font-weight:700;">K</td>
                                  <td style="padding-left:10px;font-size:18px;font-weight:700;color:#0f2a21;">Klare</td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#ffffff;border-radius:16px;padding:36px 32px;box-shadow:0 1px 3px rgba(15,42,33,0.06);">
                              %s
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 8px;text-align:center;">
                              <p style="margin:0;font-size:12px;line-height:1.6;color:#9aa69f;">
                                Klare — Your money, already sorted.<br>
                                Powered by Moolre
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(escape(title), escape(preheader), body + fallback);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
