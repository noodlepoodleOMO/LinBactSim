% plot_msd.m
% Individual + ensemble MSD from a LinBactSim "MSD Positions" export
% (Analysis.exportMsdPositions: col 1 = time (s), cols 2:end = one
% bacterium each, cells "[row, col]", two rows per iteration).
%
% Bacteria reach the exit at different times, and their trajectory is
% then frozen (repeated position) for the rest of the export. Freezing
% them into the ensemble average would drag MSD(tau) down for large tau,
% since exited bacteria stop displacing. Standard fix (used here): treat
% each bacterium's trajectory as censored at its exit time, and at each
% lag tau only average over bacteria whose valid (pre-exit) length still
% covers tau -- a shrinking cohort as tau grows, instead of a biased one.

file = 'msd_positions.xlsx';   % <-- path to the exported file

T = readcell(file);
time = cell2mat(T(2:end, 1));
posCells = T(2:end, 2:end);
[nT, nB] = size(posCells);

pos = nan(nT, nB, 2);   % pos(t,b,1)=row, pos(t,b,2)=col
for b = 1:nB
    for t = 1:nT
        v = posCells{t, b};
        if ~(ischar(v) || isstring(v)), continue; end
        nums = sscanf(char(v), '[%f, %f]');
        if numel(nums) == 2, pos(t, b, :) = nums; end
    end
end

% Valid length per bacterium: trim the trailing repeated (post-exit) run.
validLen = repmat(nT, 1, nB);
for b = 1:nB
    last = squeeze(pos(nT, b, :));
    for t = nT-1:-1:1
        if isequal(squeeze(pos(t, b, :)), last)
            validLen(b) = t;
        else
            break;
        end
    end
end

% Individual time-averaged MSD, each computed only over its own valid range.
maxLag = max(validLen) - 1;
msdIndiv = nan(maxLag, nB);
for b = 1:nB
    L = validLen(b);
    x = pos(1:L, b, 1); y = pos(1:L, b, 2);
    for lag = 1:(L - 1)
        dx = x(1+lag:end) - x(1:end-lag);
        dy = y(1+lag:end) - y(1:end-lag);
        msdIndiv(lag, b) = mean(dx.^2 + dy.^2);
    end
end

% Ensemble MSD: average across bacteria still "alive" (not yet exited) at each lag.
msdEnsemble = nan(maxLag, 1);
for lag = 1:maxLag
    alive = validLen > lag;
    if any(alive), msdEnsemble(lag) = mean(msdIndiv(lag, alive)); end
end

lagTime = (1:maxLag)' * (time(2) - time(1));

%% Plot 1: individual (grey) + ensemble (teal) MSD
figure; hold on;
nShow = min(60, nB);
for b = 1:nShow
    plot(lagTime, msdIndiv(:, b), 'Color', [0.75 0.75 0.75], 'HandleVisibility', 'off');
end
plot(lagTime, msdEnsemble, 'Color', '#2596be', 'LineWidth', 2.5, 'DisplayName', 'Ensemble MSD');
legend; xlabel('lag time (s)'); ylabel('MSD (px^2)');
title(sprintf('Individual (grey, n=%d) and ensemble MSD', nShow));
box on;

%% Plot 2: log-log MSD with diffusive/superdiffusive/ballistic reference slopes
figure; hold on;
for b = 1:nShow
    loglog(lagTime, msdIndiv(:, b), 'Color', [0.75 0.75 0.75], 'HandleVisibility', 'off');
end
loglog(lagTime, msdEnsemble, 'Color', '#2596be', 'LineWidth', 2.5, 'DisplayName', 'Ensemble MSD');

% Reference lines, anchored to the ensemble MSD at the first valid lag so
% their slope is directly comparable to the data.
refIdx = find(~isnan(msdEnsemble), 1);
slopes = [1, 1.5, 2];
styles = {':', '--', '-.'};
for s = 1:numel(slopes)
    ref = msdEnsemble(refIdx) * (lagTime / lagTime(refIdx)) .^ slopes(s);
    loglog(lagTime, ref, styles{s}, 'Color', [0.3 0.3 0.3], ...
        'DisplayName', sprintf('slope %.1f', slopes(s)));
end
set(gca, 'XScale', 'log', 'YScale', 'log');
legend show; xlabel('lag time (s)'); ylabel('MSD (px^2)');
title('MSD, log-log');

%% Plot 3: ensemble MSD stratified by exit-time quartile
% The shrinking-cohort average above still mixes populations: if exit time
% correlates with the dynamics (e.g. faster movers leave sooner), the
% surviving cohort at large tau is not a random subsample, it's whichever
% bacteria happened to be slow/trapped. Standard fix (as with probes
% leaving the field of view in microrheology): bin trajectories by how
% long they survived and average each cohort separately.
nBins = 4;
[~, order] = sort(validLen);   % ascending: fastest-exiting first
edges = round(linspace(0, nB, nBins + 1));
binColors = {'#1b9e77', '#d95f02', '#7570b3', '#e7298a'};

figure; hold on;
for k = 1:nBins
    members = order(edges(k)+1 : edges(k+1));
    binValidLen = validLen(members);
    binMaxLag = max(binValidLen) - 1;
    binMsd = nan(binMaxLag, 1);
    for lag = 1:binMaxLag
        alive = binValidLen > lag;
        if any(alive), binMsd(lag) = mean(msdIndiv(lag, members(alive))); end
    end
    loglog(lagTime(1:binMaxLag), binMsd, 'Color', binColors{k}, 'LineWidth', 2, ...
        'DisplayName', sprintf('exit quartile %d (n=%d)', k, numel(members)));
end
set(gca, 'XScale', 'log', 'YScale', 'log');
legend show; xlabel('lag time (s)'); ylabel('MSD (px^2)');
title('Ensemble MSD by exit-time quartile');

%% Plot 4: two example trajectories (no maze background needed)
figure; hold on;
pick = [1, 2];
colors = {'#d95f02', '#7570b3'};
for k = 1:numel(pick)
    b = pick(k);
    L = validLen(b);
    plot(pos(1:L, b, 2), pos(1:L, b, 1), 'Color', colors{k}, 'LineWidth', 1.5, ...
        'DisplayName', sprintf('Bacterium %d', b));
end
set(gca, 'YDir', 'reverse');
axis equal; xlabel('col (px)'); ylabel('row (px)');
legend show; title('Example trajectories');
