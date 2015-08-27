clear all;
matfiles = dir(fullfile('D:', 'tempWork', 'mutations', '*.ARRAY'));

for i=1:1:size(matfiles,1)
    a=textread(matfiles(i).name);
    a=sortrows(a,1);
    f1=figure
    
    plot(a(:,1),a(:,2));
    title(matfiles(i).name);
    s=strcat(matfiles(i).name,'.png');
    saveas(f1, s,'png');
end