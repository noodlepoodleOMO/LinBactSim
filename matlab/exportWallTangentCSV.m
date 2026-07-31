function exportWallTangentCSV(wallOutline, filename)
%EXPORTWALLTANGENTCSV Write a dense wall-tangent-angle CSV for LinBactSim.
%   EXPORTWALLTANGENTCSV(wallOutline, filename) calls WALLTANGENTANGLES on
%   wallOutline and writes the result as one CSV line per maze row, one
%   comma-separated angle (degrees) per column, with the literal text
%   "NaN" for non-wall pixels -- same row/col shape as the maze itself, so
%   the Java side can load it straight in and index it by pixel.

    angleDeg = wallTangentAngles(wallOutline);
    [nRows, nCols] = size(angleDeg);

    fid = fopen(filename, 'w');
    if fid == -1
        error('exportWallTangentCSV:fopen', 'Could not open %s for writing', filename);
    end
    cleanupObj = onCleanup(@() fclose(fid)); %#ok<NASGU>

    for r = 1:nRows
        vals = arrayfun(@formatAngle, angleDeg(r, :), 'UniformOutput', false);
        fprintf(fid, '%s\n', strjoin(vals, ','));
    end
end

function s = formatAngle(v)
    if isnan(v)
        s = 'NaN';
    else
        s = sprintf('%.4f', v);
    end
end
