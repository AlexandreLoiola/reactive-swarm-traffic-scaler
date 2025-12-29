FROM nginx:1.27-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/nginx.conf
COPY upstream.conf /etc/nginx/conf.d/upstream.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]