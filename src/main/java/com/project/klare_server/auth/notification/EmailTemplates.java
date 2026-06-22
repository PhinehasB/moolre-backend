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
