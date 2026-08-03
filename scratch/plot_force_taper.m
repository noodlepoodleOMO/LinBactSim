% plot_force_taper.m
% Visualize the sum-of-Gaussians force function for each species, with and
% without the 40->50 um half-cosine taper used in ForceModel4RayCutoff.
% All species overlaid on one plot; lighter shade = no taper, full color = tapered.

species = {'VN', 'MM', 'PP', 'VF', 'EC'};

fits.VN = [ -0.0036,  1.2507,  0.9317;
             0.0715,  2.8665,  3.8596;
             0.0071,  7.8100, 12.8844;
            -0.0528,  2.0906,  4.7022;
             0.0068, 40.2948, 45.2815];

fits.MM = [ 0.0153,  2.5950,  1.9702;
           -0.0040, 11.3104,  6.4047;
            0.0186, 14.9915, 33.8388;
           -0.0095, 28.9726, 14.7796];

fits.PP = [ 0.0122,  0.5554,  1.5147;
            0.0136,  3.4549,  3.8106;
            0.0036,  9.3358,  2.6879;
            0.0010, 27.6339,  3.2085;
            0.0087, 30.0462,  9.8569;
            0.0108, 56.5761, 17.3077;
            0.0099, 14.2141,  7.0088];

fits.VF = [-0.2726,  1.1716,  1.5535;
           -0.0054, 19.2327, 19.7369;
            0.3078,  1.0795,  1.6279;
            21.0348, -689.0934, 260.3118];

fits.EC = [ 0.0385,  2.0206,  1.5073;
            0.0097,  4.5895,  4.0643;
            0.0083, 37.4501, 18.5767;
            0.0060, 11.4156, 10.8695];

% tab10-derived colors, matching BacteriumSpecies.java (VN, MM, PP, VF, EC)
colors = [ 31, 119, 180;
          214,  39,  40;
          188, 189,  34;
          148, 103, 189;
           44, 160,  44] / 255;
lightColors = colors + (1 - colors) * 0.65;  % lighter tint for no-taper curves

TAPER_START_UM = 40.0;
FORCE_CUTOFF_UM = 50.0;

x = linspace(0.00001, 60, 2000);

figure; hold on;
for s = 1:numel(species)
    coefs = fits.(species{s});
    F = force_function(x, coefs);
    Ft = F .* taper(x, TAPER_START_UM, FORCE_CUTOFF_UM);

    plot(x, F,  'Color', lightColors(s,:), 'LineWidth', 1.5, 'HandleVisibility', 'off');
    plot(x, Ft, 'Color', colors(s,:),      'LineWidth', 2,   'DisplayName', species{s});
end
yline(0, '--k');
xline(TAPER_START_UM, ':k', 'HandleVisibility', 'off');
xline(FORCE_CUTOFF_UM, ':k', 'HandleVisibility', 'off');
legend; xlabel('distance from wall (\mum)'); ylabel('F (k_BT / \mum)');
title('Force: raw (light) vs. 40\rightarrow50 \mum taper (bold)');
xlim([0 60]); ylim([-0.8 1.2]);

function F = force_function(x, coeffs)
    % Mirrors BacteriumSpecies.forceFunction: negative gradient of a
    % sum-of-Gaussians free-energy fit.
    num = zeros(size(x));
    den = zeros(size(x));
    for k = 1:size(coeffs, 1)
        a = coeffs(k, 1); b = coeffs(k, 2); c = coeffs(k, 3);
        z = (x - b) / c;
        g = exp(-z.^2);
        num = num + (-2 * a * (x - b) / c^2) .* g;
        den = den + a * g;
    end
    F = num ./ den;
end

function w = taper(x, taperStart, cutoff)
    % Mirrors ForceModel4RayCutoff.taper: half-cosine ramp from 1 to 0.
    w = ones(size(x));
    w(x >= cutoff) = 0;
    ramp = x > taperStart & x < cutoff;
    t = (x(ramp) - taperStart) / (cutoff - taperStart);
    w(ramp) = 0.5 * (1 + cos(pi * t));
end
